<script setup>
import { reactive, ref, useId } from 'vue'
import { customerRequest } from './customerApi.js'

const props = defineProps({ customerPhone: { type: String, required: true } })
const emit = defineEmits(['saved', 'session-expired'])
const editor = ref(null)
const saving = ref(false)
const formError = ref('')
const headingId = useId()
const labelsId = useId()
const form = reactive({ id: null, contactName: '', phone: '', deliveryAddress: '', label: '' })

function open(address) {
  Object.assign(form, address ?? { id: null, contactName: '', phone: props.customerPhone, deliveryAddress: '', label: '' })
  formError.value = ''
  editor.value.showModal()
}

async function saveAddress() {
  if (saving.value) return
  saving.value = true
  formError.value = ''
  const { id, contactName, phone, deliveryAddress, label } = form
  try {
    const address = await customerRequest(id ? '/addresses/' + id : '/addresses', {
      method: id ? 'PUT' : 'POST', body: { contactName, phone, deliveryAddress, label },
    })
    editor.value.close()
    emit('saved', { edited: Boolean(id), address })
  } catch (failure) {
    if (failure.status === 401) {
      editor.value.close()
      emit('session-expired')
    } else {
      formError.value = failure.message
    }
  } finally {
    saving.value = false
  }
}

function preventClose(event) {
  if (saving.value) event.preventDefault()
}

defineExpose({ open })
</script>

<template>
    <dialog ref="editor" class="shop-dialog" :aria-labelledby="headingId" @cancel="preventClose">
      <h2 :id="headingId">{{ form.id ? '编辑地址' : '添加收货地址' }}</h2>
      <form class="shop-form" @submit.prevent="saveAddress">
        <div class="shop-address-fields">
          <label class="shop-field">
            <span>收货人</span>
            <input v-model.trim="form.contactName" maxlength="32" autocomplete="name" required autofocus :disabled="saving" />
          </label>
          <label class="shop-field">
            <span>联系电话</span>
            <input v-model.trim="form.phone" type="tel" inputmode="numeric" maxlength="11" pattern="1[3-9][0-9]{9}"
                   autocomplete="tel-national" required :disabled="saving" />
          </label>
        </div>
        <label class="shop-field">
          <span>详细地址</span>
          <textarea v-model.trim="form.deliveryAddress" maxlength="200" rows="3" autocomplete="street-address"
                    placeholder="如校区、楼栋和门牌号" required :disabled="saving"></textarea>
        </label>
        <label class="shop-field">
          <span>地址标签（可选）</span>
          <input v-model.trim="form.label" maxlength="12" :list="labelsId" placeholder="如宿舍" :disabled="saving" />
        </label>
        <datalist :id="labelsId"><option>宿舍</option><option>教学楼</option><option>家</option></datalist>
        <p v-if="formError" class="notice" role="alert">{{ formError }}</p>
        <div class="shop-dialog-actions">
          <button class="quiet-button" type="button" :disabled="saving" @click="editor.close()">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存地址' }}</button>
        </div>
      </form>
    </dialog>
</template>
