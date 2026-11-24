<script setup>
import cover from './assets/bento-editorial.jpg'
</script>

<template>
  <div class="entry-shell">
    <header class="entry-header">
      <a href="/" class="wordmark" aria-label="校园便当首页">
        <span class="brand-mark" aria-hidden="true"></span>
        校园便当
      </a>
    </header>

    <main class="entry-main">
      <section class="entry-options" aria-labelledby="entry-heading">
        <h1 id="entry-heading">选择你的入口</h1>
        <nav class="entry-roles" aria-label="选择使用身份">
          <a href="/shop/" class="entry-role entry-role-shop">
            <strong>我是顾客</strong>
            <span>浏览菜单、管理收货地址</span>
          </a>
          <a href="/admin/" class="entry-role">
            <strong>我是商家</strong>
            <span>员工登录、管理菜品和套餐</span>
          </a>
        </nav>
      </section>
      <img class="entry-photo" :src="cover" alt="" width="1122" height="1402" />
    </main>
  </div>
</template>

<style scoped>
.entry-shell {
  width: min(100% - 4rem, 70rem);
  min-height: 100svh;
  margin-inline: auto;
  display: flex;
  flex-direction: column;
}

.entry-header {
  display: flex;
  align-items: center;
  min-height: 5.125rem;
  border-bottom: 1px solid #e5ded4;
}

.entry-header a, .entry-role {
  text-decoration: none;
  color: inherit;
}

.entry-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.13fr);
  align-items: center;
  flex: 1;
  gap: clamp(2rem, 5vw, 4.5rem);
  padding-block: 2rem 2.75rem;
}

.entry-options {
  width: min(100%, 23.5rem);
  justify-self: center;
}

.entry-options h1 {
  font-size: clamp(2.2rem, 4vw, 3.25rem);
}

.entry-roles {
  display: grid;
  gap: .75rem;
  margin-top: 1.7rem;
}

.entry-role {
  display: grid;
  gap: .22rem;
  min-height: 5.35rem;
  align-content: center;
  padding: .8rem 1.1rem;
  border: 1px solid var(--border);
  border-radius: .65rem;
  background: #fffdf9;
}

.entry-role strong { font-size: 1.2rem; line-height: 1.3; }
.entry-role span { color: var(--muted); font-size: .9rem; }
.entry-role:hover { border-color: #a78272; background: #f6eee7; }
.entry-role-shop { border-color: var(--accent); background: var(--accent); color: white; }
.entry-role-shop span { color: #fff2e9; }
.entry-role-shop:hover { border-color: var(--accent-hover); background: var(--accent-hover); }

.entry-photo {
  width: 100%;
  height: clamp(23rem, 73svh, 37rem);
  border-radius: 1rem;
  object-fit: cover;
  object-position: center 55%;
}

@media (max-width: 720px) {
  .entry-shell { width: min(100% - 2rem, 28rem); }
  .entry-header { min-height: 4rem; }
  .entry-main { grid-template-columns: 1fr; align-content: start; gap: 1.35rem; padding-top: 1.15rem; }
  .entry-photo { grid-row: 1; height: clamp(10rem, 48vw, 15rem); object-position: center 59%; }
  .entry-options { width: 100%; }
  .entry-roles { margin-top: 1rem; }
}
</style>
