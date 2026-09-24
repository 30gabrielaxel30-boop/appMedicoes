/* Service worker: guarda a "casca" do app para abrir mesmo sem internet.
   Os dados continuam vindo do Supabase; registros feitos offline ficam na fila do app. */
const CACHE='refeicoes-shell-v7';
const SHELL=['./','./index.html','./config.js','./manifest.json','./icon.svg','./icon-192.png'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(SHELL)).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{
  const u=new URL(e.request.url);
  if(e.request.method!=='GET')return;
  const isShell=u.origin===location.origin&&!u.pathname.includes('/rest/')&&!u.pathname.includes('/auth/');
  const isLib=/cdn\.jsdelivr\.net|cdnjs\.cloudflare\.com|fonts\.(googleapis|gstatic)\.com/.test(u.host);
  if(!isShell&&!isLib)return; // Supabase e demais: sempre rede
  e.respondWith(caches.match(e.request).then(hit=>{
    const net=fetch(e.request).then(res=>{if(res&&(res.ok||res.type==='opaque')){const cp=res.clone();caches.open(CACHE).then(c=>c.put(e.request,cp))}return res}).catch(()=>hit);
    return isShell?net.catch(()=>hit)||hit:(hit||net);
  }));
});
