// Lamboorg Ultra desktop: the live site in its own window. The site updates itself, so this app rarely needs updating.
const { app, BrowserWindow, shell, Menu, nativeTheme, session } = require('electron');
const path = require('path');
const fs = require('fs');

const HOST = 'lamboorg.neocities.org';
const HOME = 'https://' + HOST + '/';
const isOurs = u => { try { const x = new URL(u); return x.protocol === 'https:' && x.host === HOST; } catch (e) { return false; } };

if (!app.requestSingleInstanceLock()) app.quit();
nativeTheme.themeSource = 'dark';

const stateFile = () => path.join(app.getPath('userData'), 'window.json');
const loadState = () => { try { return JSON.parse(fs.readFileSync(stateFile(), 'utf8')); } catch (e) { return { width: 1200, height: 820 }; } };

let win;
function create() {
  const st = loadState();
  win = new BrowserWindow({
    width: st.width, height: st.height, x: st.x, y: st.y, minWidth: 380, minHeight: 560,
    backgroundColor: '#050506', title: 'Lamboorg Ultra', autoHideMenuBar: true, show: false,
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: { contextIsolation: true, sandbox: true, spellcheck: true }
  });
  if (st.max) win.maximize();
  win.webContents.setUserAgent(win.webContents.getUserAgent().replace(/Electron\/\S+\s?/, '') + ' LamboorgDesktop/' + app.getVersion());
  win.once('ready-to-show', () => win.show());
  const save = () => { try { const b = win.getNormalBounds(); fs.writeFileSync(stateFile(), JSON.stringify(Object.assign(b, { max: win.isMaximized() }))); } catch (e) {} };
  win.on('close', save);

  // our site stays in the window, every other link opens in the normal browser
  win.webContents.setWindowOpenHandler(({ url }) => { if (!isOurs(url)) shell.openExternal(url); return { action: isOurs(url) ? 'allow' : 'deny' }; });
  win.webContents.on('will-navigate', (e, url) => { if (!isOurs(url) && !url.startsWith('file:')) { e.preventDefault(); shell.openExternal(url); } });
  win.webContents.on('did-fail-load', (e, code, desc, url, main) => { if (main && code !== -3) win.loadFile(path.join(__dirname, 'offline.html')); });
  win.loadURL(HOME);
}

app.whenReady().then(() => {
  // camera / mic for calls and QR, notifications, clipboard, full screen: only for the Lamboorg site
  const ok = ['media', 'notifications', 'clipboard-sanitized-write', 'clipboard-read', 'fullscreen', 'mediaKeySystem'];
  session.defaultSession.setPermissionRequestHandler((wc, perm, cb, details) => cb(ok.includes(perm) && isOurs(details.requestingUrl || wc.getURL())));
  session.defaultSession.setPermissionCheckHandler((wc, perm, origin) => ok.includes(perm) && isOurs(origin || (wc && wc.getURL()) || ''));

  const mac = process.platform === 'darwin';
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    ...(mac ? [{ role: 'appMenu' }] : []),
    { role: 'editMenu' },
    { label: 'View', submenu: [{ role: 'reload' }, { type: 'separator' }, { role: 'resetZoom' }, { role: 'zoomIn' }, { role: 'zoomOut' }, { type: 'separator' }, { role: 'togglefullscreen' }] },
    { role: 'windowMenu' }
  ]));
  create();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) create(); });
});
app.on('second-instance', () => { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
