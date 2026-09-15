import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8');
const readOrdersManager = (relativePath) =>
  readFileSync(resolve(root, '..', 'jzo2o-orders', 'jzo2o-orders-manager', relativePath), 'utf8');

test('active order list has a server-side page route and ends loading after a failed request', () => {
  const page = read('src/pages/order/orderList/index.vue');
  const controller = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersController.java'
  );
  const service = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/service/IOrdersManagerService.java'
  );
  const implementation = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java'
  );

  assert.match(controller, /@GetMapping\("\/page"\)/);
  assert.match(controller, /ordersManagerService\.operationPageQuery/);
  assert.match(service, /PageResult<Orders> operationPageQuery\(OrderPageQueryReqDTO/);
  assert.match(implementation, /PageResult<Orders> operationPageQuery\(OrderPageQueryReqDTO/);
  assert.match(page, /catch\s*\([^)]*\)\s*\{[\s\S]*listData\.value\s*=\s*\[\]/);
  assert.match(page, /finally\s*\{[\s\S]*dataLoading\.value\s*=\s*false/);
});

test('active order query ignores an empty customer phone filter', () => {
  const implementation = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java'
  );

  assert.match(
    implementation,
    /\.like\(StrUtil\.isNotBlank\(contactsPhone\),\s*Orders::getContactsPhone,\s*contactsPhone\)/
  );
});

test('active order list loads once per route entry and removes empty filters', () => {
  const page = read('src/pages/order/orderList/index.vue');
  const searchForm = read('src/pages/order/orderList/components/SearchForm.vue');

  assert.doesNotMatch(page, /watchEffect\s*\(/);
  assert.match(page, /const normalizeOrderQuery\s*=\s*\(query\)/);
  assert.match(page, /return Object\.fromEntries\(/);
  assert.match(page, /getOrderList\(normalizeOrderQuery\(query\)\)/);
  assert.match(searchForm, /ordersStatus/);
  assert.match(searchForm, /payStatus/);
  assert.match(searchForm, /refundStatus/);
});

test('operation order controller exposes detail aggregation and the existing cancel flow', () => {
  const controller = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersController.java'
  );
  const service = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/service/IOrdersManagerService.java'
  );
  const strategyManager = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/strategy/OrderCancelStrategyManager.java'
  );

  assert.match(controller, /@GetMapping\("\/aggregation\/\{id\}"\)/);
  assert.match(controller, /@PutMapping\("\/cancel"\)/);
  assert.match(controller, /ordersManagerService\.operationDetail\(id\)/);
  assert.match(controller, /ordersManagerService\.cancel\(orderCancelDTO\)/);
  assert.match(service, /OperationOrdersDetailResDTO operationDetail\(Long id\)/);
  assert.match(strategyManager, /orderCancelDTO\.setUserId\(orders\.getUserId\(\)\)/);
  assert.doesNotMatch(strategyManager, /orderCancelDTO\.setCurrentUserId\(orders\.getUserId\(\)\)/);
});

test('order table only enables eligible refunds and refreshes after a confirmed refund', () => {
  const page = read('src/pages/order/orderList/index.vue');
  const table = read('src/pages/order/orderList/components/TableList.vue');
  const dialog = read('src/pages/order/orderList/components/DialogForm.vue');

  assert.match(table, /const canRefund\s*=\s*\(row\)/);
  assert.match(table, /:aria-disabled="!canRefund\(row\)"/);
  assert.match(table, /if \(!canRefund\(val\)\) return/);
  assert.match(table, /router\.push\('\/order\/orderList\/orderDetail\/' \+ val\.id\)/);
  assert.match(page, /DialogPlugin\.confirm/);
  assert.match(page, /await refundOrder\(/);
  assert.match(page, /已发起退款，请等待支付渠道处理/);
  assert.match(page, /fetchData\(requestData\.value\)/);
  assert.doesNotMatch(dialog, /emit\('handleSubmit', formData\.value\)\s*\n\s*onClickCloseBtn\(\)/);
});

test('phone search supports a trimmed partial number and service records keep safe timestamps', () => {
  const implementation = readOrdersManager(
    'src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java'
  );
  const detail = read('src/pages/order/orderList/orderDetail.vue');

  assert.match(implementation, /String contactsPhone = StrUtil\.trim\(orderPageQueryReqDTO\.getContactsPhone\(\)\)/);
  assert.match(implementation, /\.like\(StrUtil\.isNotBlank\(contactsPhone\), Orders::getContactsPhone, contactsPhone\)/);
  assert.match(implementation, /OrdersServe ordersServe = ordersServeManagerService\.queryById\(id\)/);
  assert.doesNotMatch(implementation, /OrdersServe::getOrdersId, id/);
  assert.match(detail, /detailData\.orderInfo\?\.ordersStatus !== 0\s*"/);
  assert.doesNotMatch(detail, /detailData\.serveInfo\?\.realServeStartTime\s*"/);
  assert.match(detail, /detailData\.serveInfo\?\.realServeStartTime\s*\?\s*formatDateTimeToDateTimeString/);
  assert.match(detail, /detailData\.serveInfo\?\.realServeEndTime\s*\?\s*formatDateTimeToDateTimeString/);
});

test('admin empty state uses the new service illustration and unneeded menus are commented out', () => {
  const emptyState = read('src/components/noData/index.vue');
  const router = read('src/router/modules/components.ts');

  assert.match(emptyState, /service-empty\.png/);
  assert.match(emptyState, /objectFit: 'contain'/);
  assert.match(router, /\/\* 暂不启用：评价管理[\s\S]*?\*\//);
  assert.match(router, /\/\* 暂不启用：企业管理[\s\S]*?\*\//);
});
