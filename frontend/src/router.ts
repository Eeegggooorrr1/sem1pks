import { createRouter, createWebHistory } from 'vue-router'
import { session } from './session'
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('./pages/CatalogPage.vue'), meta: { title: 'Объявления' } },
    { path: '/listings/new', component: () => import('./pages/ListingEditorPage.vue'), meta: { title: 'Новое объявление', auth: true } },
    { path: '/listings/:id/edit', component: () => import('./pages/ListingEditorPage.vue'), meta: { title: 'Изменение объявления', auth: true } },
    { path: '/listings/:id', component: () => import('./pages/ListingPage.vue'), meta: { title: 'Объявление' } },
    { path: '/import', component: () => import('./pages/ImportPage.vue'), meta: { title: 'Импорт CSV', admin: true } },
    { path: '/analytics', component: () => import('./pages/AnalyticsPage.vue'), meta: { title: 'Аналитика', admin: true } },
    { path: '/orders', redirect: '/orders/outgoing' },
    { path: '/orders/outgoing', component: () => import('./pages/OrdersPage.vue'), props: { direction: 'OUTGOING' }, meta: { title: 'Покупки', auth: true } },
    { path: '/orders/incoming', component: () => import('./pages/OrdersPage.vue'), props: { direction: 'INCOMING' }, meta: { title: 'Продажи', auth: true } },
    { path: '/login', component: () => import('./pages/AuthPage.vue'), meta: { title: 'Вход' } },
    { path: '/register', component: () => import('./pages/AuthPage.vue'), meta: { title: 'Регистрация' } },
    { path: '/account', component: () => import('./pages/AccountPage.vue'), meta: { title: 'Аккаунт', auth: true } },
    { path: '/categories', component: () => import('./pages/CategoriesPage.vue'), meta: { title: 'Категории', admin: true } },
    { path: '/users', component: () => import('./pages/UsersPage.vue'), meta: { title: 'Пользователи', admin: true } },
    { path: '/audit', component: () => import('./pages/AuditPage.vue'), meta: { title: 'Аудит', admin: true } },
    { path: '/:pathMatch(.*)*', component: () => import('./pages/NotFoundPage.vue'), meta: { title: 'Страница не найдена' } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
router.beforeEach(to => {
  if ((to.meta.auth || to.meta.admin) && !session.account) return { path: '/login', query: { next: to.fullPath } }
  if (to.meta.admin && session.account?.role !== 'ADMIN') return '/'
})
router.afterEach(to => { document.title = `${to.meta.title} - Объявления` })
export default router
