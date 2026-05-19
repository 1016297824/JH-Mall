<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="SKU 编码" prop="skuCode">
        <el-input
          v-model="queryParams.skuCode"
          placeholder="请输入SKU 编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="SKU 销售名称" prop="skuName">
        <el-input
          v-model="queryParams.skuName"
          placeholder="请输入SKU 销售名称"
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
          v-hasPermi="['mall-product:sku:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-product:sku:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-product:sku:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-product:sku:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="skuList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="所属 SPU ID" align="center" prop="spuId" />
      <el-table-column label="SKU 编码" align="center" prop="skuCode" />
      <el-table-column label="SKU 销售名称" align="center" prop="skuName" />
      <el-table-column label="销售属性 JSON" align="center" prop="attrsJson" />
      <el-table-column label="销售价" align="center" prop="price" />
      <el-table-column label="市场价/划线价" align="center" prop="marketPrice" />
      <el-table-column label="成本价" align="center" prop="costPrice" />
      <el-table-column label="SKU 级图片" align="center" prop="image" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.image" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="重量" align="center" prop="weight" />
      <el-table-column label="该 SKU 累计销量" align="center" prop="salesCount" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-product:sku:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-product:sku:remove']">删除</el-button>
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

    <!-- 添加或修改SKU 管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="skuRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="SKU 编码" prop="skuCode">
              <el-input v-model="form.skuCode" placeholder="请输入SKU 编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SKU 销售名称" prop="skuName">
              <el-input v-model="form.skuName" placeholder="请输入SKU 销售名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="销售属性 JSON" prop="attrsJson">
              <el-input v-model="form.attrsJson" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="SKU 级图片" prop="image">
              <image-upload v-model="form.image"/>
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

<script setup lang="ts" name="Sku">
import type { MallProductSku, SkuQueryParams } from "@/types/api/mall-product/sku"
import { listSku, getSku, delSku, addSku, updateSku } from "@/api/mall-product/sku"

const { proxy } = getCurrentInstance()

const skuList = ref<MallProductSku[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallProductSku,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    spuId: undefined,
    skuCode: undefined,
    skuName: undefined,
    attrsJson: undefined,
    price: undefined,
    marketPrice: undefined,
    costPrice: undefined,
    image: undefined,
    weight: undefined,
    salesCount: undefined,
    isDeleted: undefined,
  } as SkuQueryParams,
  rules: {
    spuId: [
      { required: true, message: "所属 SPU ID不能为空", trigger: "blur" }
    ],
    skuCode: [
      { required: true, message: "SKU 编码不能为空", trigger: "blur" }
    ],
    skuName: [
      { required: true, message: "SKU 销售名称不能为空", trigger: "blur" }
    ],
    price: [
      { required: true, message: "销售价不能为空", trigger: "blur" }
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

/** 查询SKU 管理列表 */
function getList() {
  loading.value = true
  listSku(queryParams.value).then(response => {
    skuList.value = response.rows
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
    spuId: null,
    skuCode: null,
    skuName: null,
    attrsJson: null,
    price: null,
    marketPrice: null,
    costPrice: null,
    image: null,
    weight: null,
    salesCount: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("skuRef")
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
function handleSelectionChange(selection: MallProductSku[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加SKU 管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallProductSku) {
  reset()
  const _id = row.id || ids.value[0]
  getSku(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改SKU 管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["skuRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateSku(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSku(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallProductSku) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除SKU 管理编号为"' + _ids + '"的数据项？').then(function() {
    return delSku(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-product/sku/export', {
    ...queryParams.value
  }, `sku_${new Date().getTime()}.xlsx`)
}

getList()
</script>
