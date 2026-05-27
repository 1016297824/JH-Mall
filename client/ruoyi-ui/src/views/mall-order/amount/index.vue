<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
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
          v-hasPermi="['mall-admin:amount:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-admin:amount:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-admin:amount:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-admin:amount:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="amountList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单 ID" align="center" prop="orderId" />
      <el-table-column label="商品明细快照" align="center" prop="itemsJson" />
      <el-table-column label="优惠券使用快照" align="center" prop="couponSnapshotJson" />
      <el-table-column label="活动优惠快照" align="center" prop="promotionSnapshotJson" />
      <el-table-column label="积分抵扣金额" align="center" prop="pointsDiscount" />
      <el-table-column label="商品总金额" align="center" prop="totalAmount" />
      <el-table-column label="优惠总金额" align="center" prop="discountAmount" />
      <el-table-column label="运费" align="center" prop="freightAmount" />
      <el-table-column label="实付金额" align="center" prop="payAmount" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-admin:amount:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-admin:amount:remove']">删除</el-button>
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

    <!-- 添加或修改金额快照对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="amountRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="商品明细快照" prop="itemsJson">
              <el-input v-model="form.itemsJson" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券使用快照" prop="couponSnapshotJson">
              <el-input v-model="form.couponSnapshotJson" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动优惠快照" prop="promotionSnapshotJson">
              <el-input v-model="form.promotionSnapshotJson" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Amount">
import type { MallOrderAmount, AmountQueryParams } from "@/types/api/mall-order/amount"
import { listAmount, getAmount, delAmount, addAmount, updateAmount } from "@/api/mall-order/amount"

const { proxy } = getCurrentInstance()

const amountList = ref<MallOrderAmount[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallOrderAmount,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderId: undefined,
    itemsJson: undefined,
    couponSnapshotJson: undefined,
    promotionSnapshotJson: undefined,
    pointsDiscount: undefined,
    totalAmount: undefined,
    discountAmount: undefined,
    freightAmount: undefined,
    payAmount: undefined,
  } as AmountQueryParams,
  rules: {
    orderId: [
      { required: true, message: "订单 ID 不能为空", trigger: "blur" }
    ],
    itemsJson: [
      { required: true, message: "商品明细快照不能为空", trigger: "blur" }
    ],
    totalAmount: [
      { required: true, message: "商品总金额不能为空", trigger: "blur" }
    ],
    discountAmount: [
      { required: true, message: "优惠总金额不能为空", trigger: "blur" }
    ],
    freightAmount: [
      { required: true, message: "运费不能为空", trigger: "blur" }
    ],
    payAmount: [
      { required: true, message: "实付金额不能为空", trigger: "blur" }
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

/** 查询金额快照列表 */
function getList() {
  loading.value = true
  listAmount(queryParams.value).then(response => {
    amountList.value = response.rows
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
    orderId: null,
    itemsJson: null,
    couponSnapshotJson: null,
    promotionSnapshotJson: null,
    pointsDiscount: null,
    totalAmount: null,
    discountAmount: null,
    freightAmount: null,
    payAmount: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("amountRef")
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
function handleSelectionChange(selection: MallOrderAmount[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加金额快照"
}

/** 修改按钮操作 */
function handleUpdate(row: MallOrderAmount) {
  reset()
  const _id = row.id || ids.value[0]
  getAmount(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改金额快照"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["amountRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateAmount(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAmount(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallOrderAmount) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除金额快照编号为"' + _ids + '"的数据项？').then(function() {
    return delAmount(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-admin/amount/export', {
    ...queryParams.value
  }, `amount_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped lang="scss">
</style>

