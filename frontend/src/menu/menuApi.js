import { requestAdminData } from '../adminApi.js'

export async function requestMenuData(path, options) {
  try {
    return await requestAdminData(path, options)
  } catch (error) {
    if (error.status === 413) error.message = '图片文件过大，请选择不超过 5 MB 的图片。'
    throw error
  }
}

export function menuImageUrl(resource, item) {
  return '/api/admin' + resource + '/' + item.id + '/image?v=' + encodeURIComponent(item.updatedAt)
}
