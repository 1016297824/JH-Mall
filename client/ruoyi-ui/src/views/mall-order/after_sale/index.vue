<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="售后单号" prop="afterSaleNo">
        <el-input
          v-model="queryParams.afterSaleNo"
          placeholder="请输入售后单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款原因" prop="reason">
        <el-input
          v-model="queryParams.reason"
          placeholder="请输入退款原因"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请时间" prop="applyTime">
        <el-date-picker clearable
          v-model="queryParams.applyTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择申请时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="审核时间" prop="approveTime">
        <el-date-picker clearable
          v-model="queryParams.approveTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择审核时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="审核意见" prop="approveRemark">
        <el-input
          v-model="queryParams.approveRemark"
          placeholder="请输入审核意见"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货物流公司" prop="returnExpressCompany">
        <el-input
          v-model="queryParams.returnExpressCompany"
          placeholder="请输入退货物流公司"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退货物流单号" prop="returnExpressNo">
        <el-input
          v-model="queryParams.returnExpressNo"
          placeholder="请输入退货物流单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商家确认收货时间" prop="receiptTime">
        <el-date-picker clearable
          v-model="queryParams.receiptTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择商家确认收货时间">
        </el-date-picker>
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
          v-hasPermi="['mall-order:after_sale:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-order:after_sale:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-order:after_sale:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-order:after_sale:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="after_saleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="售后单号" align="center" prop="afterSaleNo" />
      <el-table-column label="关联订单 ID" align="center" prop="orderId" />
      <el-table-column label="关联订单项 ID" align="center" prop="orderItemId" />
      <el-table-column label="申请人用户 ID" align="center" prop="userId" />
      <el-table-column label="售后类型" align="center" prop="afterSaleType" />
      <el-table-column label="退款原因" align="center" prop="reason" />
      <el-table-column label="退款金额" align="center" prop="amount" />
      <el-table-column label="售后状态" align="center" prop="afterSaleStatus" />
      <el-table-column label="申请时间" align="center" prop="applyTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.applyTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" prop="approveTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.approveTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="approveRemark" />
      <el-table-column label="退货物流公司" align="center" prop="returnExpressCompany" />
      <el-table-column label="退货物流单号" align="center" prop="returnExpressNo" />
      <el-table-column label="商家确认收货时间" align="center" prop="receiptTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.receiptTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-order:after_sale:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-order:after_sale:remove']">删除</el-button>
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

    <!-- 添加或修改售后管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="after_saleRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="售后单号" prop="afterSaleNo">
              <el-input v-model="form.afterSaleNo" placeholder="请输入售后单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退款原因" prop="reason">
              <el-input v-model="form.reason" placeholder="请输入退款原因" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="申请时间" prop="applyTime">
              <el-date-picker clearable
                v-model="form.applyTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择申请时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="审核时间" prop="approveTime">
              <el-date-picker clearable
                v-model="form.approveTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择审核时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="审核意见" prop="approveRemark">
              <el-input v-model="form.approveRemark" placeholder="请输入审核意见" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货物流公司" prop="returnExpressCompany">
              <el-input v-model="form.returnExpressCompany" placeholder="请输入退货物流公司" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货物流单号" prop="returnExpressNo">
              <el-input v-model="form.returnExpressNo" placeholder="请输入退货物流单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商家确认收货时间" prop="receiptTime">
              <el-date-picker clearable
                v-model="form.receiptTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择商家确认收货时间">
              </el-date-picker>
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

<script setup lang="ts" name="After_sale">
import type { MallOrderAfterSale, After_saleQueryParams } from "@/types/api/mall-order/after_sale"
import { listAfter_sale, getAfter_sale, delAfter_sale, addAfter_sale, updateAfter_sale } from "@/api/mall-order/after_sale"

const { proxy } = getCurrentInstance()

const after_saleList = ref<MallOrderAfterSale[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallOrderAfterSale,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    afterSaleNo: undefined,
    orderId: undefined,
    orderItemId: undefined,
    userId: undefined,
    afterSaleType: undefined,
    reason: undefined,
    amount: undefined,
    afterSaleStatus: undefined,
    applyTime: undefined,
    approveTime: undefined,
    approveRemark: undefined,
    returnExpressCompany: undefined,
    returnExpressNo: undefined,
    receiptTime: undefined,
    isDeleted: undefined,
  } as After_saleQueryParams,
  rules: {
    afterSaleNo: [
      { required: true, message: "售后单号不能为空", trigger: "blur" }
    ],
    orderId: [
      { required: true, message: "关联订单 ID不能为空", trigger: "blur" }
    ],
    userId: [
      { required: true, message: "申请人用户 ID不能为空", trigger: "blur" }
    ],
    afterSaleType: [
      { required: true, message: "售后类型不能为空", trigger: "change" }
    ],
    afterSaleStatus: [
      { required: true, message: "售后状态不能为空", trigger: "change" }
    ],
    applyTime: [
      { required: true, message: "申请时间不能为空", trigger: "blur" }
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

/** 查询售后管理列表 */
function getList() {
  loading.value = true
  listAfter_sale(queryParams.value).then(response => {
    after_saleList.value = response.rows
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
    afterSaleNo: null,
    orderId: null,
    orderItemId: null,
    userId: null,
    afterSaleType: null,
    reason: null,
    amount: null,
    afterSaleStatus: null,
    applyTime: null,
    approveTime: null,
    approveRemark: null,
    returnExpressCompany: null,
    returnExpressNo: null,
    receiptTime: null,
    isDeleted: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("after_saleRef")
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
function handleSelectionChange(selection: MallOrderAfterSale[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加售后管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallOrderAfterSale) {
  reset()
  const _id = row.id || ids.value[0]
  getAfter_sale(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改售后管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["after_saleRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateAfter_sale(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAfter_sale(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallOrderAfterSale) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除售后管理编号为"' + _ids + '"的数据项？').then(function() {
    return delAfter_sale(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-order/after_sale/export', {
    ...queryParams.value
  }, `after_sale_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped lang="scss">
</style>
