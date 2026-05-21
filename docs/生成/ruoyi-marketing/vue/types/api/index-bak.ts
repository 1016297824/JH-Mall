/**
 * API 类型统一导出
 */
....

// 防止覆盖，需手动追加下面代码到index.ts文件中，追加好后此文件可删除

// mall-marketing 模块
export * from "./mall-marketing/coupon";
export * from "./mall-marketing/promotion";