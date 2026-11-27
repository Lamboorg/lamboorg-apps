// Lamboorg Ultra desktop: the live site in its own window. The site updates itself, so this app rarely needs updating.
const { app, BrowserWindow, shell, Menu, nativeTheme, session } = require('electron');
const path = require('path');
const fs = require('fs');

const HOST = 'lamboorg.neocities.org';
const HOME = 'https://' + HOST + '/';
const isOurs = u => { try { const x = new URL(u); return x.protocol === 'https:' && x.host === HOST; } catch (e) { return false; } };

// Linux: one fixed id for the window (Wayland app_id / X11 WM_CLASS), so the dock and taskbar find our icon
if (process.platform === 'linux') { app.commandLine.appendSwitch('class', 'lamboorg-ultra'); try { app.setDesktopName('lamboorg-ultra.desktop'); } catch (e) {} }
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

// AppImage has no installer: put a launcher entry + icon in the user's menu, so the app shows up with its icon
// (also fixes the generic gear icon in the dock). Runs on every start, so it follows the file if it's moved.
function linuxIntegrate() {
  const img = process.env.APPIMAGE; if (process.platform !== 'linux' || !img) return;
  try {
    const home = app.getPath('home'), share = process.env.XDG_DATA_HOME || path.join(home, '.local', 'share');
    const iconDir = path.join(share, 'icons', 'hicolor', '512x512', 'apps'), appDir = path.join(share, 'applications');
    fs.mkdirSync(iconDir, { recursive: true }); fs.mkdirSync(appDir, { recursive: true });
    fs.copyFileSync(path.join(__dirname, 'build', 'icon.png'), path.join(iconDir, 'lamboorg-ultra.png'));
    const q = s => '"' + s.replace(/(["`$\\])/g, '\\$1') + '"';
    const entry = ['[Desktop Entry]', 'Type=Application', 'Name=Lamboorg Ultra', 'Comment=Lamboorg Ultra', 'Exec=' + q(img) + ' %U', 'Icon=lamboorg-ultra',
      'Terminal=false', 'Categories=Network;Chat;', 'StartupWMClass=lamboorg-ultra', 'StartupNotify=true', ''].join('\n');
    const file = path.join(appDir, 'lamboorg-ultra.desktop');
    let old = ''; try { old = fs.readFileSync(file, 'utf8'); } catch (e) {}
    if (old !== entry) fs.writeFileSync(file, entry, { mode: 0o755 });
  } catch (e) { /* not important enough to stop the app */ }
}

app.whenReady().then(() => {
  linuxIntegrate();
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
