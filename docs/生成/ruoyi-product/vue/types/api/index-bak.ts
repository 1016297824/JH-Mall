/**
 * API 类型统一导出
 */
....

// 防止覆盖，需手动追加下面代码到index.ts文件中，追加好后此文件可删除

// mall-product 模块
export * from "./mall-product/brand";
export * from "./mall-product/category";
export * from "./mall-product/sku";
export * from "./mall-product/stock";
export * from "./mall-product/spu";