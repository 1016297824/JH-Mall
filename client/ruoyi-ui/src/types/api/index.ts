/**
 * API 类型统一导出
 */
export * from "./common";

// 登录模块
export * from "./login";
export * from "./menu";

// System 模块
export * from "./system/user";
export * from "./system/role";
export * from "./system/menu";
export * from "./system/dept";
export * from "./system/post";
export * from "./system/dict";
export * from "./system/config";
export * from "./system/notice";
export * from "./system/logininfor";
export * from "./system/operlog";

// monitor 模块
export * from "./monitor/job";
export * from "./monitor/jobLog";
export * from "./monitor/online";

// 代码生成模块
export * from "./tool/gen";

// mall-user 模块
// export * from "./mall-user/user";
export type { MallUser } from "./mall-user/user";
export * from "./mall-user/address";
export * from "./mall-user/growth_log";
export * from "./mall-user/member";
export * from "./mall-user/level";
export * from "./mall-user/account";
export * from "./mall-user/points_log";

// mall-product 模块
export * from "./mall-product/brand";
export * from "./mall-product/category";
export * from "./mall-product/sku";
export * from "./mall-product/stock";
export * from "./mall-product/spu";