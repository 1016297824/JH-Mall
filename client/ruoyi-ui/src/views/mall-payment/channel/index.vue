<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="渠道编码" prop="channelCode">
        <el-input
          v-model="queryParams.channelCode"
          placeholder="请输入渠道编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="渠道展示名称" prop="channelName">
        <el-input
          v-model="queryParams.channelName"
          placeholder="请输入渠道展示名称"
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
          v-hasPermi="['mall-admin:channel:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-admin:channel:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-admin:channel:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-admin:channel:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="channelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />

      <el-table-column label="渠道编码" align="center" prop="channelCode" />
      <el-table-column label="渠道展示名称" align="center" prop="channelName" />
      <el-table-column label="渠道类型" align="center" prop="channelType" />
      <el-table-column label="渠道配置" align="center" prop="configJson" />
      <el-table-column label="是否启用" align="center" prop="isEnabled" />
      <el-table-column label="排序值" align="center" prop="sortOrder" />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-admin:channel:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-admin:channel:remove']">删除</el-button>
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

    <!-- 添加或修改支付渠道对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="channelRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="渠道编码" prop="channelCode">
              <el-input v-model="form.channelCode" placeholder="请输入渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道展示名称" prop="channelName">
              <el-input v-model="form.channelName" placeholder="请输入渠道展示名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道配置" prop="configJson">
              <el-input v-model="form.configJson" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Channel">
import type { MallPaymentChannel, ChannelQueryParams } from "@/types/api/mall-payment/channel"
import { listChannel, getChannel, delChannel, addChannel, updateChannel } from "@/api/mall-payment/channel"

const { proxy } = getCurrentInstance()

const channelList = ref<MallPaymentChannel[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallPaymentChannel,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    channelCode: undefined,
    channelName: undefined,
    channelType: undefined,
    configJson: undefined,
    isEnabled: undefined,
    sortOrder: undefined
  } as ChannelQueryParams,
  rules: {
    channelCode: [
      { required: true, message: "渠道编码不能为空", trigger: "blur" }
    ],
    channelName: [
      { required: true, message: "渠道展示名称不能为空", trigger: "blur" }
    ],
    channelType: [
      { required: true, message: "渠道类型不能为空", trigger: "change" }
    ],
    configJson: [
      { required: true, message: "渠道配置不能为空", trigger: "blur" }
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

/** 查询支付渠道列表 */
function getList() {
  loading.value = true
  listChannel(queryParams.value).then(response => {
    channelList.value = response.rows
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
    channelCode: null,
    channelName: null,
    channelType: null,
    configJson: null,
    isEnabled: null,
    sortOrder: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("channelRef")
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
function handleSelectionChange(selection: MallPaymentChannel[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加支付渠道"
}

/** 修改按钮操作 */
function handleUpdate(row: MallPaymentChannel) {
  reset()
  const _id = row.id || ids.value[0]
  getChannel(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改支付渠道"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["channelRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateChannel(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addChannel(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallPaymentChannel) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除支付渠道编号为"' + _ids + '"的数据项？').then(function() {
    return delChannel(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-admin/channel/export', {
    ...queryParams.value
  }, `channel_${new Date().getTime()}.xlsx`)
}

getList()
</script>

