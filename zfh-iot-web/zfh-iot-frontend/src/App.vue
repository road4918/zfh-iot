<template>
  <router-view />
</template>

<script setup>
</script>

<style>
:root {
  --el-color-primary: #07c160;
  --el-color-primary-light-3: #45d585;
  --el-color-primary-light-5: #7ee3a8;
  --el-color-primary-light-7: #b0efca;
  --el-color-primary-light-8: #cdf5dd;
  --el-color-primary-light-9: #e6fbef;
  --el-color-primary-dark-2: #06ad56;
  --el-bg-color: #f7f8fa;
  --el-bg-color-page: #f7f8fa;
  --el-fill-color-blank: #ffffff;
  --el-text-color-primary: #191919;
  --el-text-color-regular: #576b95;
  --el-text-color-secondary: #8a94a6;
  --el-border-color: #e6e9f0;
  --el-border-color-light: #edf0f5;
  --el-border-radius-base: 12px;
  --el-border-radius-small: 10px;
  --el-border-radius-round: 999px;
  --el-box-shadow-light: 0 8px 24px rgba(25, 25, 25, 0.06);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  min-height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #f7f8fa;
  color: #191919;
}

#app {
  height: 100vh;
}

.app-container,
.dashboard-container,
.error-page {
  padding: 4px;
}

.app-container .el-card,
.dashboard-container .el-card,
.login-form {
  border: 1px solid #edf0f5;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(25, 25, 25, 0.05);
}

.el-card__header {
  padding: 18px 20px;
  border-bottom: 1px solid #f1f3f7;
}

.el-card__body {
  padding: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-form-item__label {
  color: #576b95;
  font-weight: 500;
}

.el-input__wrapper,
.el-select__wrapper,
.el-textarea__inner {
  border-radius: 12px;
  box-shadow: none;
  min-height: 44px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.el-input__wrapper,
.el-select__wrapper {
  padding: 0 14px;
}

.el-textarea__inner {
  padding: 12px 14px;
}

.el-input__wrapper.is-focus,
.el-select__wrapper.is-focused,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 3px rgba(7, 193, 96, 0.12);
}

.el-button {
  min-height: 40px;
  border-radius: 12px;
  font-weight: 500;
  padding: 10px 18px;
}

.el-button--primary {
  box-shadow: 0 6px 16px rgba(7, 193, 96, 0.2);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 0;
  margin-bottom: 8px;
}

.search-form .el-form-item:last-child {
  margin-right: 0;
}

.pagination-container {
  padding-top: 8px;
}

.el-table {
  --el-table-header-bg-color: #f7f8fa;
  --el-table-row-hover-bg-color: #f2fbf6;
  border-radius: 12px;
  overflow: hidden;
}
</style>
