<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="支付单号，格式 PAY + 时间戳 + 随机数" prop="paymentNo">
        <el-input
          v-model="queryParams.paymentNo"
          placeholder="请输入支付单号，格式 PAY + 时间戳 + 随机数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联订单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入关联订单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付渠道编码" prop="channelCode">
        <el-input
          v-model="queryParams.channelCode"
          placeholder="请输入支付渠道编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="渠道侧支付单号，对账用" prop="channelPaymentNo">
        <el-input
          v-model="queryParams.channelPaymentNo"
          placeholder="请输入渠道侧支付单号，对账用"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付成功时间" prop="paySuccessTime">
        <el-date-picker clearable
          v-model="queryParams.paySuccessTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择支付成功时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="支付过期时间" prop="expireTime">
        <el-date-picker clearable
          v-model="queryParams.expireTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择支付过期时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="幂等键" prop="idempotentKey">
        <el-input
          v-model="queryParams.idempotentKey"
          placeholder="请输入幂等键"
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
          v-hasPermi="['mall-payment:payment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-payment:payment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-payment:payment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-payment:payment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="paymentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="支付单号，格式 PAY + 时间戳 + 随机数" align="center" prop="paymentNo" />
      <el-table-column label="关联订单号" align="center" prop="orderNo" />
      <el-table-column label="付款用户 ID" align="center" prop="userId" />
      <el-table-column label="支付金额" align="center" prop="payAmount" />
      <el-table-column label="支付渠道编码" align="center" prop="channelCode" />
      <el-table-column label="渠道侧支付单号，对账用" align="center" prop="channelPaymentNo" />
      <el-table-column label="渠道侧支付状态" align="center" prop="channelPayStatus" />
      <el-table-column label="支付单状态" align="center" prop="paymentStatus" />
      <el-table-column label="支付成功时间" align="center" prop="paySuccessTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.paySuccessTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付过期时间" align="center" prop="expireTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="异步通知地址" align="center" prop="notifyUrl" />
      <el-table-column label="幂等键" align="center" prop="idempotentKey" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="乐观锁版本号" align="center" prop="version" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-payment:payment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-payment:payment:remove']">删除</el-button>
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

    <!-- 添加或修改支付单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="paymentRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="支付单号，格式 PAY + 时间戳 + 随机数" prop="paymentNo">
              <el-input v-model="form.paymentNo" placeholder="请输入支付单号，格式 PAY + 时间戳 + 随机数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联订单号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="请输入关联订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付渠道编码" prop="channelCode">
              <el-input v-model="form.channelCode" placeholder="请输入支付渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道侧支付单号，对账用" prop="channelPaymentNo">
              <el-input v-model="form.channelPaymentNo" placeholder="请输入渠道侧支付单号，对账用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付成功时间" prop="paySuccessTime">
              <el-date-picker clearable
                v-model="form.paySuccessTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择支付成功时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付过期时间" prop="expireTime">
              <el-date-picker clearable
                v-model="form.expireTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择支付过期时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="异步通知地址" prop="notifyUrl">
              <el-input v-model="form.notifyUrl" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="幂等键" prop="idempotentKey">
              <el-input v-model="form.idempotentKey" placeholder="请输入幂等键" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">退款单信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddMallPaymentRefund">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteMallPaymentRefund">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="mallPaymentRefundList" @selection-change="handleMallPaymentRefundSelectionChange" ref="mallPaymentRefund">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" width="60">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="退款单号，格式 REF + 时间戳 + 随机数" prop="refundNo" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.refundNo" placeholder="请输入退款单号，格式 REF + 时间戳 + 随机数" />
            </template>
          </el-table-column>
          <el-table-column label="关联订单号" prop="orderNo" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.orderNo" placeholder="请输入关联订单号" />
            </template>
          </el-table-column>
          <el-table-column label="关联售后单号" prop="afterSaleNo" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.afterSaleNo" placeholder="请输入关联售后单号" />
            </template>
          </el-table-column>
          <el-table-column label="退款金额（单位：分）" prop="refundAmount" width="120">
            <template #default="scope">
              <el-input v-model.number="scope.row.refundAmount" placeholder="请输入退款金额" />
            </template>
          </el-table-column>
          <el-table-column label="退款原因" prop="refundReason" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.refundReason" placeholder="请输入退款原因" />
            </template>
          </el-table-column>
          <el-table-column label="退款渠道编码，必须与原始支付渠道一致" prop="channelCode" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.channelCode" placeholder="请输入退款渠道编码，必须与原始支付渠道一致" />
            </template>
          </el-table-column>
          <el-table-column label="渠道侧退款单号，对账用" prop="channelRefundNo" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.channelRefundNo" placeholder="请输入渠道侧退款单号，对账用" />
            </template>
          </el-table-column>
          <el-table-column label="渠道侧退款状态" prop="channelRefundStatus" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.channelRefundStatus" placeholder="请选择渠道侧退款状态">
                <el-option label="请选择字典生成" value="" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="退款单状态" prop="refundStatus" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.refundStatus" placeholder="请选择退款单状态">
                <el-option label="请选择字典生成" value="" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="退款成功时间" prop="refundSuccessTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.refundSuccessTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择退款成功时间">
              </el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="幂等键" prop="idempotentKey" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.idempotentKey" placeholder="请输入幂等键" />
            </template>
          </el-table-column>
        </el-table>
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

<script setup lang="ts" name="Payment">
import type { MallPayment, MallPaymentRefund, PaymentQueryParams } from "@/types/api/mall-payment/payment"
import { listPayment, getPayment, delPayment, addPayment, updatePayment } from "@/api/mall-payment/payment"

const { proxy } = getCurrentInstance()

const paymentList = ref<MallPayment[]>([])
const mallPaymentRefundList = ref([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const checkedMallPaymentRefund = ref([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallPayment,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    paymentNo: undefined,
    orderNo: undefined,
    userId: undefined,
    payAmount: undefined,
    channelCode: undefined,
    channelPaymentNo: undefined,
    channelPayStatus: undefined,
    paymentStatus: undefined,
    paySuccessTime: undefined,
    expireTime: undefined,
    notifyUrl: undefined,
    idempotentKey: undefined,
    isDeleted: undefined,
    version: undefined
  } as PaymentQueryParams,
  rules: {
    paymentNo: [
      { required: true, message: "支付单号，格式 PAY + 时间戳 + 随机数不能为空", trigger: "blur" }
    ],
    orderNo: [
      { required: true, message: "关联订单号不能为空", trigger: "blur" }
    ],
    userId: [
      { required: true, message: "付款用户 ID不能为空", trigger: "blur" }
    ],
    payAmount: [
      { required: true, message: "支付金额不能为空", trigger: "blur" }
    ],
    channelCode: [
      { required: true, message: "支付渠道编码不能为空", trigger: "blur" }
    ],
    paymentStatus: [
      { required: true, message: "支付单状态不能为空", trigger: "change" }
    ],
    expireTime: [
      { required: true, message: "支付过期时间不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "修改时间不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询支付单列表 */
function getList() {
  loading.value = true
  listPayment(queryParams.value).then(response => {
    paymentList.value = response.rows
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
    orderNo: null,
    userId: null,
    payAmount: null,
    channelCode: null,
    channelPaymentNo: null,
    channelPayStatus: null,
    paymentStatus: null,
    paySuccessTime: null,
    expireTime: null,
    notifyUrl: null,
    idempotentKey: null,
    isDeleted: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null,
    version: null
  }
  mallPaymentRefundList.value = []
  proxy.resetForm("paymentRef")
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
function handleSelectionChange(selection: MallPayment[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加支付单"
}

/** 修改按钮操作 */
function handleUpdate(row: MallPayment) {
  reset()
  const _id = row.id || ids.value[0]
  getPayment(_id).then(response => {
    form.value = response.data
    mallPaymentRefundList.value = response.data?.mallPaymentRefundList ?? []
    open.value = true
    title.value = "修改支付单"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["paymentRef"].validate((valid: boolean) => {
    if (valid) {
      form.value.mallPaymentRefundList = mallPaymentRefundList.value
      if (form.value.id != null) {
        updatePayment(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPayment(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallPayment) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除支付单编号为"' + _ids + '"的数据项？').then(function() {
    return delPayment(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 退款单添加按钮操作 */
function handleAddMallPaymentRefund() {
  let obj: MallPaymentRefund = {}
  obj.refundNo = undefined
  obj.orderNo = undefined
  obj.afterSaleNo = undefined
  obj.userId = undefined
  obj.refundAmount = undefined
  obj.refundReason = undefined
  obj.channelCode = undefined
  obj.channelRefundNo = undefined
  obj.channelRefundStatus = undefined
  obj.refundStatus = undefined
  obj.refundSuccessTime = undefined
  obj.idempotentKey = undefined
  obj.isDeleted = undefined
  obj.version = undefined
  mallPaymentRefundList.value.push(obj)
}

/** 退款单删除按钮操作 */
function handleDeleteMallPaymentRefund() {
  if (checkedMallPaymentRefund.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的退款单数据")
  } else {
    const mallPaymentRefunds = mallPaymentRefundList.value
    const checkedMallPaymentRefunds = checkedMallPaymentRefund.value
    mallPaymentRefundList.value = mallPaymentRefunds.filter(function(item: any) {
      return checkedMallPaymentRefunds.indexOf(item.index) == -1
    })
  }
}

/** 复选框选中数据 */
function handleMallPaymentRefundSelectionChange(selection: any[]) {
  checkedMallPaymentRefund.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-payment/payment/export', {
    ...queryParams.value
  }, `payment_${new Date().getTime()}.xlsx`)
}

getList()
</script>
