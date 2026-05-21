<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="优惠券名称" prop="couponName">
        <el-input
          v-model="queryParams.couponName"
          placeholder="请输入优惠券名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="有效期开始时间" prop="useStartTime">
        <el-date-picker clearable
          v-model="queryParams.useStartTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择有效期开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="有效期截止时间" prop="useEndTime">
        <el-date-picker clearable
          v-model="queryParams.useEndTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择有效期截止时间">
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
          v-hasPermi="['mall-marketing:coupon:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-marketing:coupon:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-marketing:coupon:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-marketing:coupon:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="couponList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="优惠券名称" align="center" prop="couponName" />
      <el-table-column label="优惠券类型" align="center" prop="couponType" />
      <el-table-column label="优惠面值" align="center" prop="faceValue" />
      <el-table-column label="折扣率" align="center" prop="discountRate" />
      <el-table-column label="折扣上限" align="center" prop="discountLimit" />
      <el-table-column label="最低订单金额门槛" align="center" prop="minOrderAmount" />
      <el-table-column label="发行总量" align="center" prop="totalCount" />
      <el-table-column label="剩余可领取数量" align="center" prop="remainCount" />
      <el-table-column label="每人限领数量" align="center" prop="perUserLimit" />
      <el-table-column label="有效期开始时间" align="center" prop="useStartTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.useStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期截止时间" align="center" prop="useEndTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.useEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="优惠券状态" align="center" prop="couponStatus" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-marketing:coupon:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-marketing:coupon:remove']">删除</el-button>
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

    <!-- 添加或修改优惠券定义对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="couponRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="优惠券名称" prop="couponName">
              <el-input v-model="form.couponName" placeholder="请输入优惠券名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="有效期开始时间" prop="useStartTime">
              <el-date-picker clearable
                v-model="form.useStartTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择有效期开始时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="有效期截止时间" prop="useEndTime">
              <el-date-picker clearable
                v-model="form.useEndTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择有效期截止时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">用户优惠券记录信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddMallMarketingCouponRecord">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteMallMarketingCouponRecord">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="mallMarketingCouponRecordList" @selection-change="handleMallMarketingCouponRecordSelectionChange" ref="mallMarketingCouponRecord">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" width="60">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="优惠券编码" prop="couponCode" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.couponCode" placeholder="请输入优惠券编码" />
            </template>
          </el-table-column>
          <el-table-column label="记录状态" prop="recordStatus" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.recordStatus" placeholder="请选择记录状态">
                <el-option label="请选择字典生成" value="" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="使用/锁定的订单号" prop="orderNo" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.orderNo" placeholder="请输入使用/锁定的订单号" />
            </template>
          </el-table-column>
          <el-table-column label="锁定时间" prop="lockTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.lockTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择锁定时间">
              </el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="使用时间" prop="useTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.useTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择使用">
              </el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="释放时间" prop="releaseTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.releaseTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择释放时间">
              </el-date-picker>
            </template>
          </el-table-column>
          <el-table-column label="过期时间" prop="expireTime" width="240">
            <template #default="scope">
              <el-date-picker clearable
                v-model="scope.row.expireTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择过期时间">
              </el-date-picker>
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

<script setup lang="ts" name="Coupon">
import type { MallMarketingCoupon, MallMarketingCouponRecord, CouponQueryParams } from "@/types/api/mall-marketing/coupon"
import { listCoupon, getCoupon, delCoupon, addCoupon, updateCoupon } from "@/api/mall-marketing/coupon"

const { proxy } = getCurrentInstance()

const couponList = ref<MallMarketingCoupon[]>([])
const mallMarketingCouponRecordList = ref([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const checkedMallMarketingCouponRecord = ref([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallMarketingCoupon,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    couponName: undefined,
    couponType: undefined,
    faceValue: undefined,
    discountRate: undefined,
    discountLimit: undefined,
    minOrderAmount: undefined,
    totalCount: undefined,
    remainCount: undefined,
    perUserLimit: undefined,
    useStartTime: undefined,
    useEndTime: undefined,
    couponStatus: undefined
  } as CouponQueryParams,
  rules: {
    couponName: [
      { required: true, message: "优惠券名称不能为空", trigger: "blur" }
    ],
    couponType: [
      { required: true, message: "优惠券类型不能为空", trigger: "change" }
    ],
    faceValue: [
      { required: true, message: "优惠面值不能为空", trigger: "blur" }
    ],
    totalCount: [
      { required: true, message: "发行总量，0=不限量不能为空", trigger: "blur" }
    ],
    remainCount: [
      { required: true, message: "剩余可领取数量不能为空", trigger: "blur" }
    ],
    useStartTime: [
      { required: true, message: "有效期开始时间不能为空", trigger: "blur" }
    ],
    useEndTime: [
      { required: true, message: "有效期截止时间不能为空", trigger: "blur" }
    ],
    couponStatus: [
      { required: true, message: "优惠券状态不能为空", trigger: "change" }
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

/** 查询优惠券定义列表 */
function getList() {
  loading.value = true
  listCoupon(queryParams.value).then(response => {
    couponList.value = response.rows
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
    couponName: null,
    couponType: null,
    faceValue: null,
    discountRate: null,
    discountLimit: null,
    minOrderAmount: null,
    totalCount: null,
    remainCount: null,
    perUserLimit: null,
    useStartTime: null,
    useEndTime: null,
    couponStatus: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null
  }
  mallMarketingCouponRecordList.value = []
  proxy.resetForm("couponRef")
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
function handleSelectionChange(selection: MallMarketingCoupon[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加优惠券定义"
}

/** 修改按钮操作 */
function handleUpdate(row: MallMarketingCoupon) {
  reset()
  const _id = row.id || ids.value[0]
  getCoupon(_id).then(response => {
    form.value = response.data
    mallMarketingCouponRecordList.value = response.data?.mallMarketingCouponRecordList ?? []
    open.value = true
    title.value = "修改优惠券定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["couponRef"].validate((valid: boolean) => {
    if (valid) {
      form.value.mallMarketingCouponRecordList = mallMarketingCouponRecordList.value
      if (form.value.id != null) {
        updateCoupon(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCoupon(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallMarketingCoupon) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除优惠券定义编号为"' + _ids + '"的数据项？').then(function() {
    return delCoupon(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 用户优惠券记录添加按钮操作 */
function handleAddMallMarketingCouponRecord() {
  let obj: MallMarketingCouponRecord = {}
  obj.userId = undefined
  obj.couponCode = undefined
  obj.recordStatus = undefined
  obj.orderNo = undefined
  obj.faceValue = undefined
  obj.lockTime = undefined
  obj.useTime = undefined
  obj.releaseTime = undefined
  obj.expireTime = undefined
  mallMarketingCouponRecordList.value.push(obj)
}

/** 用户优惠券记录删除按钮操作 */
function handleDeleteMallMarketingCouponRecord() {
  if (checkedMallMarketingCouponRecord.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的用户优惠券记录数据")
  } else {
    const mallMarketingCouponRecords = mallMarketingCouponRecordList.value
    const checkedMallMarketingCouponRecords = checkedMallMarketingCouponRecord.value
    mallMarketingCouponRecordList.value = mallMarketingCouponRecords.filter(function(item: any) {
      return checkedMallMarketingCouponRecords.indexOf(item.index) == -1
    })
  }
}

/** 复选框选中数据 */
function handleMallMarketingCouponRecordSelectionChange(selection: any[]) {
  checkedMallMarketingCouponRecord.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-marketing/coupon/export', {
    ...queryParams.value
  }, `coupon_${new Date().getTime()}.xlsx`)
}

getList()
</script>
