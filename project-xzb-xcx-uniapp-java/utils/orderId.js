// 订单号可能超过 JavaScript 的安全整数范围，页面间传递时必须保留字符串形式。
export const keepOrderId = (id) => (id === undefined || id === null || id === '' ? null : String(id));
