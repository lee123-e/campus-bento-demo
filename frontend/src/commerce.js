const currency = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })

export function formatMoney(value) {
  return currency.format(value)
}

export function showFlavors(flavors) {
  return flavors.map(item => item.name + ' ' + item.option).join(' · ')
}

export function orderStatus(status) {
  return ['待支付', '待派送', '已派送', '已完成', '已取消'][status - 1] ?? '状态待确认'
}

export function orderTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}
