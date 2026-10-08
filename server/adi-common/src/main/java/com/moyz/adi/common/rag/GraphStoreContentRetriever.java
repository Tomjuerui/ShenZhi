package com.moyz.adi.common.rag;

import com.moyz.adi.common.cosntant.AdiConstant;
import com.moyz.adi.common.dto.RefGraphDto;
import com.moyz.adi.common.exception.BaseException;
import com.moyz.adi.common.util.AdiStringUtil;
import com.moyz.adi.common.vo.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.moyz.adi.common.enums.ErrorEnum.B_BREAK_SEARCH;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class GraphStoreContentRetriever implements ContentRetriever {
    public static final Function<Query, Integer> DEFAULT_MAX_RESULTS = query -> 3;
    public static final Function<Query, Filter> DEFAULT_FILTER = query -> null;

    public static final String DEFAULT_DISPLAY_NAME = "Default";
    public static final int DEFAULT_MAX_HOPS = 3;

    private final GraphStore graphStore;
    private final ChatModel chatModel;

    private final Function<Query, Integer> maxResultsProvider;
    private final Function<Query, Filter> filterProvider;

    private final String displayName;

    private final boolean breakIfSearchMissed;

    private final int maxHops;

    private final Set<String> excludedItemUuids;

    private final RefGraphDto kbQaRecordRefGraphDto = RefGraphDto.builder().vertices(Collections.emptyList()).edges(Collections.emptyList()).entitiesFromQuestion(Collections.emptyList()).build();

    @Builder
    private GraphStoreContentRetriever(String displayName,
                                       GraphStore graphStore,
                                       ChatModel chatModel,
                                       Function<Query, Integer> dynamicMaxResults,
                                       Function<Query, Filter> dynamicFilter,
                                       Boolean breakIfSearchMissed,
                                       Integer maxHops,
                                       Set<String> excludedItemUuids) {
        this.displayName = getOrDefault(displayName, DEFAULT_DISPLAY_NAME);
        this.graphStore = ensureNotNull(graphStore, "graphStore");
        this.chatModel = ensureNotNull(chatModel, "ChatModel");
        this.maxResultsProvider = getOrDefault(dynamicMaxResults, DEFAULT_MAX_RESULTS);
        this.filterProvider = getOrDefault(dynamicFilter, DEFAULT_FILTER);
        this.breakIfSearchMissed = breakIfSearchMissed;
        this.maxHops = (maxHops == null) ? DEFAULT_MAX_HOPS : maxHops;
        this.excludedItemUuids = excludedItemUuids;
    }

    public static GraphStoreContentRetriever from(GraphStore graphStore) {
        return builder().graphStore(graphStore).build();
    }

    @Override
    public List<Content> retrieve(Query query) {
        log.info("Graph retrieve,query:{}", query);
        String response = "";
        try {
            response = chatModel.chat(GraphExtractPrompt.GRAPH_EXTRACTION_PROMPT.replace("{input_text}", query.text()));
        } catch (Exception e) {
            log.error("Graph retrieve. extract graph error", e);
        }
        if (StringUtils.isBlank(response)) {
            return Collections.emptyList();
        }
        Set<String> entities = new HashSet<>();
        String[] records = response.split(AdiConstant.GRAPH_RECORD_DELIMITER);
        for (String record : records) {
            String newRecord = record.replaceAll("^\\(|\\)$", "");
            String[] recordAttributes = newRecord.split(AdiConstant.GRAPH_TUPLE_DELIMITER);
            if (recordAttributes.length >= 4 && (recordAttributes[0].contains("\"entity\"") || recordAttributes[0].contains("\"实体\""))) {
                entities.add(AdiStringUtil.clearStr(recordAttributes[1].toUpperCase()));
            } else if (recordAttributes.length >= 4 && (recordAttributes[0].contains("\"relationship\"") || recordAttributes[0].contains("\"关系\""))) {
                String sourceName = AdiStringUtil.clearStr(recordAttributes[1].toUpperCase());
                String targetName = AdiStringUtil.clearStr(recordAttributes[2].toUpperCase());
                entities.add(AdiStringUtil.clearStr(sourceName));
                entities.add(AdiStringUtil.clearStr(targetName));
            }
        }
//Determine whether to forcibly interrupt the query, if no match then stop further operations
        //判断是否要强行中断查询，没有命中则不再进行下一步操作（比如说请求LLM），直接抛出异常中断流程
        if (breakIfSearchMissed && entities.isEmpty()) {
            log.warn("Graph search missed");
            throw new BaseException(B_BREAK_SEARCH);
        }
        entities = entities.stream().map(AdiStringUtil::removeSpecialChar).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (entities.isEmpty()) {
            log.info("No entities parsed from user query");
            return Collections.emptyList();
        }

        Filter filter = filterProvider.apply(query);
        int perHopLimit = Math.max(maxResultsProvider.apply(query), 10);
        Set<String> frontier = entities;
        Map<String, GraphVertex> visitedVertices = new LinkedHashMap<>();
        Map<String, GraphEdge> visitedEdges = new LinkedHashMap<>();

        for (int hop = 0; hop < maxHops && !frontier.isEmpty(); hop++) {
            List<String> frontierNames = new ArrayList<>(frontier);
            List<GraphVertex> hopVertices = graphStore.searchVertices(
                    GraphVertexSearch.builder()
                            .names(frontierNames)
                            .metadataFilter(filter)
                            .limit(perHopLimit)
                            .build()
            );
            for (GraphVertex vertex : hopVertices) {
                visitedVertices.put(vertex.getId(), vertex);
            }

            List<Triple<GraphVertex, GraphEdge, GraphVertex>> hopEdges = new ArrayList<>();
            hopEdges.addAll(graphStore.searchEdges(
                    GraphEdgeSearch.builder()
                            .source(GraphSearchCondition.builder().names(frontierNames).metadataFilter(filter).build())
                            .limit(perHopLimit)
                            .build()
            ));
            hopEdges.addAll(graphStore.searchEdges(
                    GraphEdgeSearch.builder()
                            .target(GraphSearchCondition.builder().names(frontierNames).metadataFilter(filter).build())
                            .limit(perHopLimit)
                            .build()
            ));

            Set<String> next = new HashSet<>();
            for (Triple<GraphVertex, GraphEdge, GraphVertex> triple : hopEdges) {
                GraphEdge edge = triple.getMiddle();
                visitedEdges.putIfAbsent(edge.getId(), edge);
                visitedVertices.putIfAbsent(triple.getLeft().getId(), triple.getLeft());
                visitedVertices.putIfAbsent(triple.getRight().getId(), triple.getRight());
                if (StringUtils.isNotBlank(triple.getLeft().getName())) {
                    next.add(triple.getLeft().getName().toUpperCase());
                }
                if (StringUtils.isNotBlank(triple.getRight().getName())) {
                    next.add(triple.getRight().getName().toUpperCase());
                }
            }
            next.removeAll(frontier);
            for (GraphVertex vertex : visitedVertices.values()) {
                if (StringUtils.isNotBlank(vertex.getName())) {
                    next.remove(vertex.getName().toUpperCase());
                }
            }
            frontier = next;
        }

        // Post-filter: exclude vertices/edges whose kb_item_uuid is entirely disabled.
        // Cannot use IsNotIn in the graph query because kb_item_uuid may be a
        // comma-separated multi-value string (GraphStoreIngestor append logic).
        if (excludedItemUuids != null && !excludedItemUuids.isEmpty()) {
            visitedVertices.values().removeIf(v -> !shouldKeep(v.getMetadata()));
            visitedEdges.values().removeIf(e -> !shouldKeep(e.getMetadata()));
        }

        kbQaRecordRefGraphDto.setEntitiesFromQuestion(entities.stream().toList());
        kbQaRecordRefGraphDto.setVertices(new ArrayList<>(visitedVertices.values()));
        kbQaRecordRefGraphDto.setEdges(new ArrayList<>(visitedEdges.values()));

        List<Content> contents = new ArrayList<>();
        for (GraphVertex vertex : visitedVertices.values()) {
            if (StringUtils.isNotBlank(vertex.getDescription())) {
                contents.add(Content.from(vertex.getDescription()));
            }
        }
        for (GraphEdge edge : visitedEdges.values()) {
            if (StringUtils.isNotBlank(edge.getDescription())) {
                contents.add(Content.from(edge.getDescription()));
            }
        }
        return contents;
    }

    public RefGraphDto getGraphRef() {
        return kbQaRecordRefGraphDto;
    }

    /**
     * Check whether a graph element (vertex/edge) should be kept after disabled-document
     * filtering. The metadata kb_item_uuid may be comma-separated (due to GraphStoreIngestor
     * append logic); keep the element as long as at least one associated document is enabled.
     */
    private boolean shouldKeep(Map<String, Object> metadata) {
        if (metadata == null || excludedItemUuids == null || excludedItemUuids.isEmpty()) {
            return true;
        }
        Object raw = metadata.get(AdiConstant.MetadataKey.KB_ITEM_UUID);
        if (raw == null) {
            return true;
        }
        Set<String> itemUuids = Arrays.stream(String.valueOf(raw).split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(toSet());
        return itemUuids.stream().anyMatch(u -> !excludedItemUuids.contains(u));
    }

    public static class GraphStoreContentRetrieverBuilder {

        public GraphStoreContentRetrieverBuilder maxResults(Integer maxResults) {
            if (maxResults != null) {
                dynamicMaxResults = (query) -> ensureGreaterThanZero(maxResults, "maxResults");
            }
            return this;
        }

        public GraphStoreContentRetrieverBuilder filter(Filter filter) {
            if (filter != null) {
                dynamicFilter = (query) -> filter;
            }
            return this;
        }

        public GraphStoreContentRetrieverBuilder breakIfSearchMissed(boolean breakFlag) {
            breakIfSearchMissed = breakFlag;
            return this;
        }
    }

    @Override
    public String toString() {
        return "GraphStoreContentRetriever{" +
               "displayName='" + displayName + '\'' +
               '}';
    }

}
