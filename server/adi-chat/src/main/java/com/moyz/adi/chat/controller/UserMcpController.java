package com.moyz.adi.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyz.adi.common.base.ThreadContext;
import com.moyz.adi.common.dto.UserMcpDto;
import com.moyz.adi.common.dto.mcp.McpAddOrEditReq;
import com.moyz.adi.common.dto.mcp.UserMcpUpdateReq;
import com.moyz.adi.common.entity.Mcp;
import com.moyz.adi.common.service.McpService;
import com.moyz.adi.common.service.UserMcpService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/mcp")
public class UserMcpController {

    @Resource
    private UserMcpService userMcpService;

    @Resource
    private McpService mcpService;

    @Operation(summary = "当前登录用户启用的MCP列表 | Current User's Enabled MCP List")
    @GetMapping(value = "/list")
    public Page<UserMcpDto> listByUserId(@NotNull @Min(1) Integer currentPage, @NotNull @Min(10) Integer pageSize) {
        return userMcpService.searchByUserId(ThreadContext.getCurrentUserId(), currentPage, pageSize);
    }

    @PostMapping("/saveOrUpdate")
    public UserMcpDto saveOrUpdate(@Validated @RequestBody UserMcpUpdateReq userMcpUpdateReq) {
        return userMcpService.saveOrUpdate(userMcpUpdateReq);
    }

    @Operation(summary = "用户自建MCP列表 | Current User's Own MCP List")
    @GetMapping(value = "/ownList")
    public List<Mcp> ownList() {
        return mcpService.listUserOwn(ThreadContext.getCurrentUserId());
    }

    @Operation(summary = "用户新增MCP | User Add MCP")
    @PostMapping("/add")
    public Mcp add(@Validated @RequestBody McpAddOrEditReq req) {
        Mcp mcp = mcpService.addOrUpdateUserOwn(req, false);
        UserMcpUpdateReq enableReq = new UserMcpUpdateReq();
        enableReq.setMcpId(mcp.getId());
        enableReq.setIsEnable(true);
        userMcpService.saveOrUpdate(enableReq);
        return mcp;
    }

    @Operation(summary = "用户编辑MCP | User Edit MCP")
    @PostMapping("/edit")
    public Mcp edit(@Validated @RequestBody McpAddOrEditReq req) {
        return mcpService.addOrUpdateUserOwn(req, false);
    }

    @Operation(summary = "用户删除MCP | User Delete MCP")
    @PostMapping("/del/{uuid}")
    public boolean del(@PathVariable String uuid) {
        mcpService.softDeleteUserOwn(uuid);
        return true;
    }

}
