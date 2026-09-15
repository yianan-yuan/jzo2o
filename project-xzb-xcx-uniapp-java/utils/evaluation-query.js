/**
 * 构造统一评价分页参数。
 * “全部”不能传 evaluationType，避免小程序把 null 序列化成实际筛选条件。
 */
export const buildEvaluationPageParams = ({ pageNo = 1, pageSize = 10, evaluationType } = {}) => {
  const params = { pageNo, pageSize };
  if (evaluationType === 1 || evaluationType === 2) {
    params.evaluationType = evaluationType;
  }
  return params;
};
