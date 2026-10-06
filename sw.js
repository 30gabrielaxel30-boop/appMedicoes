/* Service worker: guarda a "casca" do app para abrir mesmo sem internet.
   Os dados continuam vindo do Supabase; registros feitos offline ficam na fila do app. */
const CACHE='refeicoes-shell-2.6.0';
const SHELL=['./', './index.html', './config.js', './manifest.json', './icon-192.png', './icon-512.png', './icon-maskable-512.png', './apple-touch-icon.png', './app-2.6.0.js', './lib/supabase.js', './lib/jspdf.umd.min.js', './lib/jspdf.plugin.autotable.min.js', './lib/jsQR.js', './lib/fflate.js', './fonts/barlow-semi-condensed-latin-500-normal.woff2', './fonts/barlow-semi-condensed-latin-600-normal.woff2', './fonts/barlow-semi-condensed-latin-700-normal.woff2', './fonts/ibm-plex-mono-latin-400-normal.woff2', './fonts/ibm-plex-mono-latin-500-normal.woff2', './fonts/ibm-plex-sans-latin-400-normal.woff2', './fonts/ibm-plex-sans-latin-500-normal.woff2', './fonts/ibm-plex-sans-latin-600-normal.woff2'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>Promise.all(SHELL.map(u=>c.add(u).catch(()=>null)))).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{
  const u=new URL(e.request.url);
  if(e.request.method!=='GET')return;
  const isShell=u.origin===location.origin&&!u.pathname.includes('/rest/')&&!u.pathname.includes('/auth/');
  if(!isShell)return; // Supabase: sempre rede (bibliotecas e fontes agora são do próprio site)
  // rede primeiro (para pegar a versão nova); se a rede falhar ou responder com erro (ex.: arquivo ainda não publicado), usa a cópia guardada
  e.respondWith(caches.match(e.request).then(hit=>fetch(e.request).then(res=>{
    if(res&&res.ok){const cp=res.clone();caches.open(CACHE).then(c=>c.put(e.request,cp));return res}
    return hit||res;
  }).catch(()=>hit||Response.error())));
});
