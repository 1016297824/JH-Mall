<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="SPU 名称" prop="spuName">
        <el-input
          v-model="queryParams.spuName"
          placeholder="请输入SPU 名称"
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
          v-hasPermi="['mall-product:spu:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-product:spu:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-product:spu:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-product:spu:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="spuList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="所属三级类目 ID" align="center" prop="categoryId" />
      <el-table-column label="所属品牌 ID" align="center" prop="brandId" />
      <el-table-column label="SPU 名称" align="center" prop="spuName" />
      <el-table-column label="商品详情描述" align="center" prop="spuDescription" />
      <el-table-column label="商品主图 URL" align="center" prop="mainImage" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.mainImage" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="轮播图 JSON 数组" align="center" prop="imagesJson" />
      <el-table-column label="最低销售价" align="center" prop="priceMin" />
      <el-table-column label="最高销售价" align="center" prop="priceMax" />
      <el-table-column label="累计销量" align="center" prop="salesCount" />
      <el-table-column label="评价条数" align="center" prop="reviewCount" />
      <el-table-column label="上下架状态" align="center" prop="publishStatus" />
      <el-table-column label="审核状态" align="center" prop="verifyStatus" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="乐观锁版本号" align="center" prop="version" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-product:spu:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-product:spu:remove']">删除</el-button>
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

    <!-- 添加或修改SPU 管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="spuRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="SPU 名称" prop="spuName">
              <el-input v-model="form.spuName" placeholder="请输入SPU 名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品详情描述" prop="spuDescription">
              <el-input v-model="form.spuDescription" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品主图 URL" prop="mainImage">
              <image-upload v-model="form.mainImage"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="轮播图 JSON 数组" prop="imagesJson">
              <el-input v-model="form.imagesJson" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Spu">
import type { MallProductSpu, SpuQueryParams } from "@/types/api/mall-product/spu"
import { listSpu, getSpu, delSpu, addSpu, updateSpu } from "@/api/mall-product/spu"

const { proxy } = getCurrentInstance()

const spuList = ref<MallProductSpu[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallProductSpu,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryId: undefined,
    brandId: undefined,
    spuName: undefined,
    spuDescription: undefined,
    mainImage: undefined,
    imagesJson: undefined,
    priceMin: undefined,
    priceMax: undefined,
    salesCount: undefined,
    reviewCount: undefined,
    publishStatus: undefined,
    verifyStatus: undefined,
    isDeleted: undefined,
    version: undefined
  } as SpuQueryParams,
  rules: {
    categoryId: [
      { required: true, message: "所属三级类目 ID不能为空", trigger: "blur" }
    ],
    spuName: [
      { required: true, message: "SPU 名称不能为空", trigger: "blur" }
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

/** 查询SPU 管理列表 */
function getList() {
  loading.value = true
  listSpu(queryParams.value).then(response => {
    spuList.value = response.rows
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
    categoryId: null,
    brandId: null,
    spuName: null,
    spuDescription: null,
    mainImage: null,
    imagesJson: null,
    priceMin: null,
    priceMax: null,
    salesCount: null,
    reviewCount: null,
    publishStatus: null,
    verifyStatus: null,
    isDeleted: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null,
    version: null
  }
  proxy.resetForm("spuRef")
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
function handleSelectionChange(selection: MallProductSpu[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加SPU 管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallProductSpu) {
  reset()
  const _id = row.id || ids.value[0]
  getSpu(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改SPU 管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["spuRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateSpu(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSpu(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallProductSpu) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除SPU 管理编号为"' + _ids + '"的数据项？').then(function() {
    return delSpu(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-product/spu/export', {
    ...queryParams.value
  }, `spu_${new Date().getTime()}.xlsx`)
}

getList()
</script>
