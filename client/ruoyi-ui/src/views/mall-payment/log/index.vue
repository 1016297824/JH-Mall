<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关联支付单号" prop="paymentNo">
        <el-input
          v-model="queryParams.paymentNo"
          placeholder="请输入关联支付单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联退款单号" prop="refundNo">
        <el-input
          v-model="queryParams.refundNo"
          placeholder="请输入关联退款单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="渠道编码" prop="channelCode">
        <el-input
          v-model="queryParams.channelCode"
          placeholder="请输入渠道编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理完成时间" prop="processTime">
        <el-date-picker clearable
          v-model="queryParams.processTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择处理完成时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="处理结果说明" prop="processResult">
        <el-input
          v-model="queryParams.processResult"
          placeholder="请输入处理结果说明"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="回调防重放 nonce" prop="nonce">
        <el-input
          v-model="queryParams.nonce"
          placeholder="请输入回调防重放 nonce"
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
          v-hasPermi="['mall-payment:log:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-payment:log:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-payment:log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-payment:log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="关联支付单号" align="center" prop="paymentNo" />
      <el-table-column label="关联退款单号" align="center" prop="refundNo" />
      <el-table-column label="渠道编码" align="center" prop="channelCode" />
      <el-table-column label="回调类型" align="center" prop="callbackType" />
      <el-table-column label="原始回调报文 JSON" align="center" prop="rawBody" />
      <el-table-column label="验签结果" align="center" prop="isVerified" />
      <el-table-column label="处理状态" align="center" prop="processStatus" />
      <el-table-column label="处理完成时间" align="center" prop="processTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.processTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="处理结果说明" align="center" prop="processResult" />
      <el-table-column label="回调防重放 nonce" align="center" prop="nonce" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-payment:log:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-payment:log:remove']">删除</el-button>
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

    <!-- 添加或修改回调日志对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="logRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="关联支付单号" prop="paymentNo">
              <el-input v-model="form.paymentNo" placeholder="请输入关联支付单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联退款单号" prop="refundNo">
              <el-input v-model="form.refundNo" placeholder="请输入关联退款单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道编码" prop="channelCode">
              <el-input v-model="form.channelCode" placeholder="请输入渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原始回调报文 JSON" prop="rawBody">
              <el-input v-model="form.rawBody" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="处理完成时间" prop="processTime">
              <el-date-picker clearable
                v-model="form.processTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择处理完成时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="处理结果说明" prop="processResult">
              <el-input v-model="form.processResult" placeholder="请输入处理结果说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="回调防重放 nonce" prop="nonce">
              <el-input v-model="form.nonce" placeholder="请输入回调防重放 nonce" />
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

<script setup lang="ts" name="Log">
import type { MallPaymentCallbackLog, LogQueryParams } from "@/types/api/mall-payment/log"
import { listLog, getLog, delLog, addLog, updateLog } from "@/api/mall-payment/log"

const { proxy } = getCurrentInstance()

const logList = ref<MallPaymentCallbackLog[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallPaymentCallbackLog,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    paymentNo: undefined,
    refundNo: undefined,
    channelCode: undefined,
    callbackType: undefined,
    rawBody: undefined,
    isVerified: undefined,
    processStatus: undefined,
    processTime: undefined,
    processResult: undefined,
    nonce: undefined,
    isDeleted: undefined,
  } as LogQueryParams,
  rules: {
    channelCode: [
      { required: true, message: "渠道编码不能为空", trigger: "blur" }
    ],
    callbackType: [
      { required: true, message: "回调类型不能为空", trigger: "change" }
    ],
    rawBody: [
      { required: true, message: "原始回调报文 JSON不能为空", trigger: "blur" }
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

/** 查询回调日志列表 */
function getList() {
  loading.value = true
  listLog(queryParams.value).then(response => {
    logList.value = response.rows
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
    paymentNo: null,
    refundNo: null,
    channelCode: null,
    callbackType: null,
    rawBody: null,
    isVerified: null,
    processStatus: null,
    processTime: null,
    processResult: null,
    nonce: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("logRef")
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
function handleSelectionChange(selection: MallPaymentCallbackLog[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加回调日志"
}

/** 修改按钮操作 */
function handleUpdate(row: MallPaymentCallbackLog) {
  reset()
  const _id = row.id || ids.value[0]
  getLog(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改回调日志"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["logRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateLog(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLog(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallPaymentCallbackLog) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除回调日志编号为"' + _ids + '"的数据项？').then(function() {
    return delLog(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-payment/log/export', {
    ...queryParams.value
  }, `log_${new Date().getTime()}.xlsx`)
}

getList()
</script>
