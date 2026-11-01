export const orderStatuses = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PAID', label: '待发货' },
  { value: 'DELIVERING', label: '待收货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已关闭' },
]

export const orderStatusMap = {
  PENDING_PAYMENT: { label: '待付款', color: '#ff8a3d' },
  PAID: { label: '待发货', color: '#1f8a4c' },
  DELIVERING: { label: '待收货', color: '#2d7ff9' },
  COMPLETED: { label: '已完成', color: '#6b7280' },
  CANCELLED: { label: '已关闭', color: '#9ca3af' },
}

export const deliveryTypes = {
  DELIVERY: '到家配送',
  PICKUP: '到店自提',
}
