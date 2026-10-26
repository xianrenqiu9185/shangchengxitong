export const orderStatusMap = {
  PENDING_PAYMENT: { label: '待付款', type: 'warning' },
  PENDING_SHIPMENT: { label: '待发货', type: 'primary' },
  PENDING_RECEIPT: { label: '待收货', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已关闭', type: 'info' },
}

export const afterSaleStatusMap = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已同意', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

export const deliveryTypes = {
  DELIVERY: '到家配送',
  PICKUP: '到店自提',
}
