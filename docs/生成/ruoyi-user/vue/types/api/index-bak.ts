/**
 * API 类型统一导出
 */
....

// 防止覆盖，需手动追加下面代码到index.ts文件中，追加好后此文件可删除

// mall-user 模块
export * from "./mall-user/user";
export * from "./mall-user/address";
export * from "./mall-user/growth_log";
export * from "./mall-user/member";
export * from "./mall-user/level";
export * from "./mall-user/account";
export * from "./mall-user/points_log";