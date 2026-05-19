<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="订单号，格式 JH + 时间戳 + 随机数" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入订单号，格式 JH + 时间戳 + 随机数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付成功时间" prop="payTime">
        <el-date-picker clearable
          v-model="queryParams.payTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择支付成功时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="发货时间" prop="deliveryTime">
        <el-date-picker clearable
          v-model="queryParams.deliveryTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择发货时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="交易完成时间" prop="completeTime">
        <el-date-picker clearable
          v-model="queryParams.completeTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择交易完成时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="取消时间" prop="cancelTime">
        <el-date-picker clearable
          v-model="queryParams.cancelTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择取消时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="取消原因" prop="cancelReason">
        <el-input
          v-model="queryParams.cancelReason"
          placeholder="请输入取消原因"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付过期时间，默认创建后 30 分钟" prop="payExpireTime">
        <el-date-picker clearable
          v-model="queryParams.payExpireTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择支付过期时间，默认创建后 30 分钟">
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
          v-hasPermi="['mall-order:order:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-order:order:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-order:order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-order:order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="订单号，格式 JH + 时间戳 + 随机数" align="center" prop="orderNo" />
      <el-table-column label="用户 ID" align="center" prop="userId" />
      <el-table-column label="订单状态" align="center" prop="orderStatus" />
      <el-table-column label="商品总金额" align="center" prop="totalAmount" />
      <el-table-column label="优惠总金额" align="center" prop="discountAmount" />
      <el-table-column label="运费金额" align="center" prop="freightAmount" />
      <el-table-column label="实付金额" align="center" prop="payAmount" />
      <el-table-column label="支付成功时间" align="center" prop="payTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.payTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发货时间" align="center" prop="deliveryTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.deliveryTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易完成时间" align="center" prop="completeTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.completeTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="取消时间" align="center" prop="cancelTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.cancelTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="取消类型" align="center" prop="cancelType" />
      <el-table-column label="取消原因" align="center" prop="cancelReason" />
      <el-table-column label="支付过期时间，默认创建后 30 分钟" align="center" prop="payExpireTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.payExpireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="买家备注" align="center" prop="remark" />
      <el-table-column label="幂等键" align="center" prop="idempotentKey" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="乐观锁版本号" align="center" prop="version" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-order:order:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-order:order:remove']">删除</el-button>
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

    <!-- 添加或修改订单管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="orderRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="订单号，格式 JH + 时间戳 + 随机数" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="请输入订单号，格式 JH + 时间戳 + 随机数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付成功时间" prop="payTime">
              <el-date-picker clearable
                v-model="form.payTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择支付成功时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发货时间" prop="deliveryTime">
              <el-date-picker clearable
                v-model="form.deliveryTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择发货时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="交易完成时间" prop="completeTime">
              <el-date-picker clearable
                v-model="form.completeTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择交易完成时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="取消时间" prop="cancelTime">
              <el-date-picker clearable
                v-model="form.cancelTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择取消时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="取消原因" prop="cancelReason">
              <el-input v-model="form.cancelReason" placeholder="请输入取消原因" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付过期时间，默认创建后 30 分钟" prop="payExpireTime">
              <el-date-picker clearable
                v-model="form.payExpireTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择支付过期时间，默认创建后 30 分钟">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="买家备注" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入买家备注" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="幂等键" prop="idempotentKey">
              <el-input v-model="form.idempotentKey" placeholder="请输入幂等键" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">订单项信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddMallOrderItem">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteMallOrderItem">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="mallOrderItemList" @selection-change="handleMallOrderItemSelectionChange" ref="mallOrderItem">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" width="60">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="SKU 编码" prop="skuCode" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.skuCode" placeholder="请输入SKU 编码" />
            </template>
          </el-table-column>
          <el-table-column label="SKU 名称" prop="skuName" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.skuName" placeholder="请输入SKU 名称" />
            </template>
          </el-table-column>
          <el-table-column label="SPU 名称" prop="spuName" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.spuName" placeholder="请输入SPU 名称" />
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

<script setup lang="ts" name="Order">
import type { MallOrder, MallOrderItem, OrderQueryParams } from "@/types/api/mall-order/order"
import { listOrder, getOrder, delOrder, addOrder, updateOrder } from "@/api/mall-order/order"

const { proxy } = getCurrentInstance()

const orderList = ref<MallOrder[]>([])
const mallOrderItemList = ref([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const checkedMallOrderItem = ref([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallOrder,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    userId: undefined,
    orderStatus: undefined,
    totalAmount: undefined,
    discountAmount: undefined,
    freightAmount: undefined,
    payAmount: undefined,
    payTime: undefined,
    deliveryTime: undefined,
    completeTime: undefined,
    cancelTime: undefined,
    cancelType: undefined,
    cancelReason: undefined,
    payExpireTime: undefined,
    idempotentKey: undefined,
    isDeleted: undefined,
    version: undefined
  } as OrderQueryParams,
  rules: {
    orderNo: [
      { required: true, message: "订单号，格式 JH + 时间戳 + 随机数不能为空", trigger: "blur" }
    ],
    userId: [
      { required: true, message: "用户 ID不能为空", trigger: "blur" }
    ],
    orderStatus: [
      { required: true, message: "订单状态不能为空", trigger: "change" }
    ],
    totalAmount: [
      { required: true, message: "商品总金额不能为空", trigger: "blur" }
    ],
    payAmount: [
      { required: true, message: "实付金额不能为空", trigger: "blur" }
    ],
    payExpireTime: [
      { required: true, message: "支付过期时间，默认创建后 30 分钟不能为空", trigger: "blur" }
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

/** 查询订单管理列表 */
function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
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
    orderNo: null,
    userId: null,
    orderStatus: null,
    totalAmount: null,
    discountAmount: null,
    freightAmount: null,
    payAmount: null,
    payTime: null,
    deliveryTime: null,
    completeTime: null,
    cancelTime: null,
    cancelType: null,
    cancelReason: null,
    payExpireTime: null,
    remark: null,
    idempotentKey: null,
    isDeleted: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null,
    version: null
  }
  mallOrderItemList.value = []
  proxy.resetForm("orderRef")
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
function handleSelectionChange(selection: MallOrder[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加订单管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallOrder) {
  reset()
  const _id = row.id || ids.value[0]
  getOrder(_id).then(response => {
    form.value = response.data
    mallOrderItemList.value = response.data?.mallOrderItemList ?? []
    open.value = true
    title.value = "修改订单管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["orderRef"].validate((valid: boolean) => {
    if (valid) {
      form.value.mallOrderItemList = mallOrderItemList.value
      if (form.value.id != null) {
        updateOrder(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addOrder(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallOrder) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除订单管理编号为"' + _ids + '"的数据项？').then(function() {
    return delOrder(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 订单项添加按钮操作 */
function handleAddMallOrderItem() {
  let obj: MallOrderItem = {}
  obj.spuId = undefined
  obj.skuId = undefined
  obj.skuCode = undefined
  obj.skuName = undefined
  obj.spuName = undefined
  obj.mainImage = undefined
  obj.attrsJson = undefined
  obj.quantity = undefined
  obj.price = undefined
  obj.totalPrice = undefined
  obj.isDeleted = undefined
  mallOrderItemList.value.push(obj)
}

/** 订单项删除按钮操作 */
function handleDeleteMallOrderItem() {
  if (checkedMallOrderItem.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的订单项数据")
  } else {
    const mallOrderItems = mallOrderItemList.value
    const checkedMallOrderItems = checkedMallOrderItem.value
    mallOrderItemList.value = mallOrderItems.filter(function(item: any) {
      return checkedMallOrderItems.indexOf(item.index) == -1
    })
  }
}

/** 复选框选中数据 */
function handleMallOrderItemSelectionChange(selection: any[]) {
  checkedMallOrderItem.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-order/order/export', {
    ...queryParams.value
  }, `order_${new Date().getTime()}.xlsx`)
}

getList()
</script>
