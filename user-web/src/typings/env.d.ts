/// <reference types="vite/client" />

interface ImportMetaEnv {
	readonly VITE_GLOB_API_URL: string;
	readonly VITE_APP_API_BASE_URL: string;
	readonly VITE_GLOB_OPEN_LONG_REPLY: string;
	readonly VITE_GLOB_APP_PWA: string;
	/** Docker dev 模式下置 true，让 Vite 用轮询监听文件（Windows bind mount 的 inotify 不可靠） */
	readonly VITE_USE_POLLING?: string;
}
