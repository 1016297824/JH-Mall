<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="收件人姓名" prop="receiverName">
        <el-input
          v-model="queryParams.receiverName"
          placeholder="请输入收件人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收件人手机号" prop="receiverPhone">
        <el-input
          v-model="queryParams.receiverPhone"
          placeholder="请输入收件人手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省" prop="province">
        <el-input
          v-model="queryParams.province"
          placeholder="请输入省"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入市"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="区" prop="district">
        <el-input
          v-model="queryParams.district"
          placeholder="请输入区"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="detailAddress">
        <el-input
          v-model="queryParams.detailAddress"
          placeholder="请输入详细地址"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮编" prop="zipCode">
        <el-input
          v-model="queryParams.zipCode"
          placeholder="请输入邮编"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="地址标签" prop="label">
        <el-input
          v-model="queryParams.label"
          placeholder="请输入地址标签"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['mall-user:address:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-user:address:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-user:address:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-user:address:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="addressList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="用户 ID" align="center" prop="userId" />
      <el-table-column label="收件人姓名" align="center" prop="receiverName" />
      <el-table-column label="收件人手机号" align="center" prop="receiverPhone" />
      <el-table-column label="省" align="center" prop="province" />
      <el-table-column label="市" align="center" prop="city" />
      <el-table-column label="区" align="center" prop="district" />
      <el-table-column label="详细地址" align="center" prop="detailAddress" />
      <el-table-column label="邮编" align="center" prop="zipCode" />
      <el-table-column label="是否默认地址" align="center" prop="isDefault" />
      <el-table-column label="地址标签" align="center" prop="label" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-user:address:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-user:address:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改地址簿对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="addressRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="收件人姓名" prop="receiverName">
              <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收件人手机号" prop="receiverPhone">
              <el-input v-model="form.receiverPhone" placeholder="请输入收件人手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="省" prop="province">
              <el-input v-model="form.province" placeholder="请输入省" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="市" prop="city">
              <el-input v-model="form.city" placeholder="请输入市" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="区" prop="district">
              <el-input v-model="form.district" placeholder="请输入区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="detailAddress">
              <el-input v-model="form.detailAddress" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮编" prop="zipCode">
              <el-input v-model="form.zipCode" placeholder="请输入邮编" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址标签" prop="label">
              <el-input v-model="form.label" placeholder="请输入地址标签" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Address">
import type { MallUserAddress, AddressQueryParams } from "@/types/api/mall-user/address"
import { listAddress, getAddress, delAddress, addAddress, updateAddress } from "@/api/mall-user/address"

const { proxy } = getCurrentInstance()

const addressList = ref<MallUserAddress[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserAddress,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    receiverName: undefined,
    receiverPhone: undefined,
    province: undefined,
    city: undefined,
    district: undefined,
    detailAddress: undefined,
    zipCode: undefined,
    isDefault: undefined,
    label: undefined,
    isDeleted: undefined,
  } as AddressQueryParams,
  rules: {
    userId: [
      { required: true, message: "用户 ID不能为空", trigger: "blur" }
    ],
    receiverName: [
      { required: true, message: "收件人姓名不能为空", trigger: "blur" }
    ],
    receiverPhone: [
      { required: true, message: "收件人手机号不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "修改时间不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询地址簿列表 */
function getList() {
  loading.value = true
  listAddress(queryParams.value).then(response => {
    addressList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    userId: null,
    receiverName: null,
    receiverPhone: null,
    province: null,
    city: null,
    district: null,
    detailAddress: null,
    zipCode: null,
    isDefault: null,
    label: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("addressRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: MallUserAddress[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加地址簿"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserAddress) {
  reset()
  const _id = row.id || ids.value[0]
  getAddress(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改地址簿"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["addressRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateAddress(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAddress(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserAddress) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除地址簿编号为"' + _ids + '"的数据项？').then(function() {
    return delAddress(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-user/address/export', {
    ...queryParams.value
  }, `address_${new Date().getTime()}.xlsx`)
}

getList()
</script>
