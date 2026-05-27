<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="品牌名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入品牌名称"
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
          v-hasPermi="['mall-admin:brand:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-admin:brand:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-admin:brand:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-admin:brand:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="brandList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />

      <el-table-column label="品牌名称" align="center" prop="name" />
      <el-table-column label="品牌 Logo" align="center" prop="logo" />
      <el-table-column label="品牌简介" align="center" prop="description" />
      <el-table-column label="排序值" align="center" prop="sortOrder" />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-admin:brand:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-admin:brand:remove']">删除</el-button>
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

    <!-- 添加或修改品牌管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="brandRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="品牌名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入品牌名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌 Logo" prop="logo">
              <el-input v-model="form.logo" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌简介" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Brand">
import type { MallProductBrand, BrandQueryParams } from "@/types/api/mall-product/brand"
import { listBrand, getBrand, delBrand, addBrand, updateBrand } from "@/api/mall-product/brand"

const { proxy } = getCurrentInstance()

const brandList = ref<MallProductBrand[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallProductBrand,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    logo: undefined,
    description: undefined,
    sortOrder: undefined,

  } as BrandQueryParams,
  rules: {
    name: [
      { required: true, message: "品牌名称不能为空", trigger: "blur" }
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

/** 查询品牌管理列表 */
function getList() {
  loading.value = true
  listBrand(queryParams.value).then(response => {
    brandList.value = response.rows
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
    name: null,
    logo: null,
    description: null,
    sortOrder: null,

    createTime: null,
    updateTime: null
  }
  proxy.resetForm("brandRef")
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
function handleSelectionChange(selection: MallProductBrand[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加品牌管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallProductBrand) {
  reset()
  const _id = row.id || ids.value[0]
  getBrand(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改品牌管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["brandRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateBrand(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addBrand(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallProductBrand) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除品牌管理编号为"' + _ids + '"的数据项？').then(function() {
    return delBrand(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-admin/brand/export', {
    ...queryParams.value
  }, `brand_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped lang="scss">
</style>

