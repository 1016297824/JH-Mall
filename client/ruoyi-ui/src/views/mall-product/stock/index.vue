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
          v-hasPermi="['mall-product:stock:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-product:stock:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-product:stock:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-product:stock:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stockList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />

      <el-table-column label="所属 SKU" align="center" prop="skuId" />
      <el-table-column label="总库存" align="center" prop="totalStock" />
      <el-table-column label="可用库存" align="center" prop="availableStock" />
      <el-table-column label="锁定库存" align="center" prop="lockedStock" />
      <el-table-column label="已售库存" align="center" prop="soldStock" />
      <el-table-column label="冻结库存" align="center" prop="frozenStock" />


      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-product:stock:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-product:stock:remove']">删除</el-button>
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

    <!-- 添加或修改库存管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="stockRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
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

<script setup lang="ts" name="Stock">
import type { MallProductSkuStock, StockQueryParams } from "@/types/api/mall-product/stock"
import { listStock, getStock, delStock, addStock, updateStock } from "@/api/mall-product/stock"

const { proxy } = getCurrentInstance()

const stockList = ref<MallProductSkuStock[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallProductSkuStock,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    skuId: undefined,
    totalStock: undefined,
    availableStock: undefined,
    lockedStock: undefined,
    soldStock: undefined,
    frozenStock: undefined,

  } as StockQueryParams,
  rules: {
    skuId: [
      { required: true, message: "所属 SKU 不能为空", trigger: "blur" }
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

/** 查询库存管理列表 */
function getList() {
  loading.value = true
  listStock(queryParams.value).then(response => {
    stockList.value = response.rows
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
    skuId: null,
    totalStock: null,
    availableStock: null,
    lockedStock: null,
    soldStock: null,
    frozenStock: null,

    createTime: null,
    updateTime: null,

  }
  proxy.resetForm("stockRef")
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
function handleSelectionChange(selection: MallProductSkuStock[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加库存管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallProductSkuStock) {
  reset()
  const _id = row.id || ids.value[0]
  getStock(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改库存管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["stockRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateStock(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addStock(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallProductSkuStock) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除库存管理编号为"' + _ids + '"的数据项？').then(function() {
    return delStock(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-product/stock/export', {
    ...queryParams.value
  }, `stock_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped lang="scss">
</style>

