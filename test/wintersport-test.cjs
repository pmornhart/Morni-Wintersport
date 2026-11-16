/**
 * Echter Test von Morni Wintersport mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node wintersport-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl, Dora, später Emil (schaut zu, fährt im
 * zweiten Wettkampf mit). Sprache und Party kommen vom Test-Netzwerk (test/fake-network), das auch Statistik, Zustand
 * und „zur Lobby“ in calls.log mitschreibt.
 *
 * Boote fahren in mineflayer nicht von selbst – dafür gibt es /wsadmin fahren (das Plugin schiebt das Boot Stück für
 * Stück durch die Tore, die Zeitnahme sieht es wie eine echte Fahrt). Biathlon: Anna läuft die erste Runde wirklich
 * (vorwärts, sprinten, lenken), sonst /wsadmin laufen. Geschossen wird überall wirklich mit dem Bogen.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25960', RCON = '127.0.0.1:26060', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const FLOOR = 100;
const SPAWN = { x: 0.5, y: 101, z: 24.5 };

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const dataDir = path.join(SERVER, 'plugins/MorniWintersport');
const playerFile = (name) => path.join(dataDir, 'spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MorniNet/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const inWorld = (c) => `execute in minecraft:wintersport run ${c}`;

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
const bots = {};
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.kickReason = null;
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 30000);
  });
  await sleep(1500);
  bots[name] = bot;
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawBar = (bot, re) => bot.bars.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const clear = (...list) =>
  list.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });

/** /wsadmin status als Werte. */
async function status() {
  const lines = (await cmd('wsadmin status'))[0].split('\n');
  const s = { top: {}, comp: {}, racers: {}, stage: {}, teams: {}, runners: {} };
  const kv = (line) => {
    const o = {};
    for (const part of line.trim().split(' ')) {
      const i = part.indexOf('=');
      if (i > 0) {
        const v = part.slice(i + 1);
        o[part.slice(0, i)] = /^-?\d+(\.\d+)?$/.test(v) ? Number(v) : v === 'true' ? true : v === 'false' ? false : v;
      }
    }
    return o;
  };
  for (const line of lines) {
    const o = kv(line);
    if ('bereit' in o) s.top = o;
    else if ('wettkampf' in o) s.comp = o;
    else if ('r' in o) s.racers[o.r] = o;
    else if ('stage' in o) s.stage = o;
    else if ('t' in o) s.teams[o.t] = o;
    else if ('b' in o) s.runners[o.b] = o;
  }
  return s;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(200);
}

async function useItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  bot.activateItem();
  await sleep(400);
}

/** Bis der Lauf/die Phase erreicht ist. */
async function waitStage(pred, ms = 30000) {
  let s = null;
  const ok = await waitFor(async () => {
    s = await status();
    return pred(s);
  }, ms, 400);
  return ok ? s : null;
}

/** Läuft der Bogen-Schuss: auf eine Stelle zielen, ganz spannen, loslassen. */
async function shoot(bot, target) {
  await bot.lookAt(target, true);
  await sleep(150);
  bot.activateItem();
  await sleep(1250);
  await bot.lookAt(target, true);
  bot.deactivateItem();
  await sleep(700);
}

const laneX = (k) => 79 + 6 * k;
const targetPoint = (lane, t) => new Vec3(laneX(lane) - 2 + t + 0.5, FLOOR + 2.5, 39.02);

/** Am Schießstand: {hits} Treffer, Rest daneben (Wand neben den Scheiben). */
async function shootRound(bot, name, hits) {
  const s = await waitStage((x) => x.runners[name]?.state === 'shooting', 60000);
  if (!s) return false;
  bot.clearControlStates();
  const lane = s.runners[name].lane - 1;
  await sleep(600);
  for (let t = 0; t < 5; t++) {
    const aim = t < hits ? targetPoint(lane, t) : new Vec3(laneX(lane) + 0.5, FLOOR + 6.5, 39.02);
    await shoot(bot, aim);
  }
  return waitFor(async () => (await status()).runners[name]?.state !== 'shooting', 15000, 300);
}

/** Wirklich laufen: Wegpunkte ablaufen (vorwärts, sprinten, hinschauen). */
async function walkPoints(bot, points, ms) {
  const start = Date.now();
  bot.setControlState('sprint', true);
  for (const p of points) {
    while (Date.now() - start < ms) {
      const pos = bot.entity.position;
      if (Math.hypot(pos.x - p.x, pos.z - p.z) < 1.6) break;
      await bot.lookAt(new Vec3(p.x, pos.y + 1.6, p.z), true);
      bot.setControlState('forward', true);
      bot.setControlState('sprint', true);
      await sleep(50);
    }
  }
  bot.clearControlStates();
  return Date.now() - start < ms;
}

/** Wegpunkte für eine Runde auf der Loipe (Uhrzeigersinn) ab der Startgeraden bis hinter die Rundenlinie. */
function loopPoints() {
  const pts = [];
  const add = (x, z) => pts.push({ x, z });
  for (let x = 104; x <= 150; x += 6) add(x, 58.5);
  add(154, 61);
  for (let z = 66; z <= 92; z += 6) add(156.5, z);
  add(153, 95);
  for (let x = 148; x >= 80; x -= 6) add(x, 96.5);
  add(77, 94);
  for (let z = 90; z >= 62; z -= 6) add(74.5, z);
  add(78, 59);
  for (let x = 84; x <= 106; x += 4) add(x, 58.5);
  return pts;
}

/** Nach dem Schießen: Strafrunden laufen (lassen), dann die nächste Runde. */
async function penaltiesThenLap(name) {
  await waitFor(async () => ['penalty', 'running'].includes((await status()).runners[name]?.state), 15000, 300);
  const pen = (await status()).runners[name]?.pen ?? 0;
  for (let k = 0; k < pen; k++) {
    await cmd(`wsadmin laufen ${name} strafrunde`);
    await waitFor(async () => ((await status()).runners[name]?.pen ?? 0) < pen - k, 15000, 300);
  }
  await waitFor(async () => (await status()).runners[name]?.state === 'running', 5000, 300);
  await cmd(`wsadmin laufen ${name} runde`);
}

/** Aussteigen wie im echten Spiel: Schleichen (Shift). */
async function sneakOut(bot) {
  bot.setControlState('sneak', true);
  await sleep(400);
  bot.setControlState('sneak', false);
}

async function main() {
  // ------------------------------------------------------------------ Dorf
  await sleep(4000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..40]')));
  check('Schrift im Dorf steht (Tafeln, Schilder)', /: (\d+)/.test(holos) && Number(holos.match(/: (\d+)/)[1]) >= 10, holos.trim());
  const gates = String(await cmd(inWorld('execute positioned -130 120 -160 if entity @e[type=block_display,distance=..200]')));
  check('Torstangen und Liftseile stehen (Anzeige-Objekte)', /: (\d+)/.test(gates) && Number(gates.match(/: (\d+)/)[1]) >= 250, gates.trim());

  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Bergdorf', near(anna.entity.position, SPAWN, 3) && anna.game.gameMode === 'adventure',
    JSON.stringify(anna.entity.position));
  const below = anna.blockAt(anna.entity.position.offset(0, -1, 0));
  check('Fester Boden unter den Füßen (Pflaster des Dorfplatzes)', below && below.name !== 'air', below?.name);
  check('Schnellleiste: Kompass, Farbstoff, Gold, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 4)?.name === 'gold_nugget' &&
      slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Abstimmen & Zuschauen“', plain(slot(anna, 0)?.customName).includes('Abstimmen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote & watch“', plain(slot(bert, 0)?.customName).includes('Vote & watch'),
    plain(slot(bert, 0)?.customName));
  check('Begrüßung + Erklärung im Chat', sees(anna, /Willkommen im Bergdorf/) && sees(anna, /Riesenslalom/));
  check('Englische Begrüßung', sees(bert, /Welcome to the mountain village/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 4000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting'));
  const day = (await status()).top.zeit;
  check('Immer Tag (Tageszeit steht)', day === 6000, String(day));

  // ------------------------------------------------------------------ Abstimmung
  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Was kommt als Nächstes?“', win && plain(win.title).includes('Was kommt als'), win && plain(win.title));
  check('Wettkampf und drei Disziplinen im Menü',
    plain(win?.slots[10]?.customName).includes('Wettkampf') && plain(win?.slots[12]?.customName).includes('Riesenslalom') &&
      plain(win?.slots[14]?.customName).includes('Biathlon') && plain(win?.slots[16]?.customName).includes('Bobbahn'),
    [10, 12, 14, 16].map((i) => plain(win?.slots[i]?.customName)).join(' / '));
  await anna.clickWindow(10, 0, 0);
  await sleep(600);
  check('Stimme für den Wettkampf', sees(anna, /Deine Stimme: Wettkampf/));
  bert.chat('/abstimmen wettkampf');
  await sleep(600);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Competition/));
  win = await openWithItem(anna, 4);
  check('Gold-Nugget öffnet „Medaillen & Bestzeiten“', win && plain(win.title).includes('Medaillen'), win && plain(win.title));
  await closeWindow(anna);

  // ------------------------------------------------------------------ Spaß im Dorf: Rodelbahn und Sessellift
  await cmd(inWorld('tp Bert -85.5 125 -285.5 180 0'));
  check('Rodelbahn: aufs Eis treten = im Schlitten', await waitFor(() => bert.vehicle != null, 4000),
    `vehicle=${bert.vehicle?.name}`);
  check('Englischer Hinweis zum Rodeln', await waitFor(() => sees(bert, /Off you go/), 2000));
  await cmd(inWorld('execute as @e[tag=wintersport_sled] run tp @s -85.5 101 -33.5'));
  check('Rodelzeit unten im Tal', await waitFor(() => sees(bert, /Toboggan time: \d+:\d\d\.\d\d/), 4000),
    bert.chats.slice(-1)[0]);
  bert.chat('/verlassen');
  await sleep(800);
  await cmd(inWorld('tp Anna -71.5 101 -32.5 180 0'));
  check('Sessellift: auf der roten Linie holt einen der Sessel ab', await waitFor(() => anna.vehicle != null, 6000),
    `vehicle=${anna.vehicle?.name}`);
  const y0 = (await status(), Number(String(await cmd('data get entity Anna Pos[1]')).match(/(-?\d+\.\d+)d/)?.[1]));
  await sleep(6000);
  const y1 = Number(String(await cmd('data get entity Anna Pos[1]')).match(/(-?\d+\.\d+)d/)?.[1]);
  check('Sessellift fährt bergauf', y1 > y0 + 0.4, `${y0} → ${y1}`);
  check('Hinweis zum Lift', sees(anna, /Gute Fahrt/));
  anna.chat('/verlassen');
  await waitFor(() => anna.vehicle == null, 3000);
  await sleep(800);
  check('/verlassen holt aus dem Lift zurück ins Dorf', near(anna.entity.position, SPAWN, 3), JSON.stringify(anna.entity.position));

  // ------------------------------------------------------------------ Saison-Schalter
  const cfgFile = path.join(dataDir, 'config.yml');
  const cfg = readFile(cfgFile);
  fs.writeFileSync(cfgFile, cfg.replace('saison: immer', 'saison: winter'));
  await cmd('wsadmin testdatum 2026-07-01', 'wsadmin neuladen');
  check('Sommer + Saison „winter“: Netzwerk-Zustand „closed“',
    await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'closed'), 4000));
  clear(anna, bert);
  await useItem(anna, 1);
  check('Bereit geht nicht: freundliche Erklärung mit Datum', sees(anna, /Sommerpause.*1\. Dezember/), anna.chats.slice(-1)[0]);
  check('Leiste im Dorf (Aktionsleiste) nennt die Pause', await waitFor(() => sawBar(bert, /Summer break.*December 1/), 3000),
    bert.bars.slice(-1)[0]);
  await cmd('wsadmin testdatum 2027-01-15');
  check('Im Januar ist offen (Zustand wieder „waiting“)', await waitFor(() => {
    const st = calls().filter((c) => c.t === 'state');
    return st.length > 0 && st[st.length - 1].state === 'waiting';
  }, 4000));
  fs.writeFileSync(cfgFile, cfg);
  await cmd('wsadmin testdatum aus', 'wsadmin neuladen');

  // ------------------------------------------------------------------ Wettkampf 1: Start
  const carl = await join('Carl');
  const dora = await join('Dora');
  clear(anna, bert, carl, dora);
  for (const b of [anna, bert, carl, dora]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
    await sleep(250);
  }
  check('Alle bereit → Countdown', await waitFor(() => sawBar(anna, /Start in \d+ s · Wettkampf/), 4000), anna.bars.slice(-1)[0]);
  let s = await waitStage((x) => x.comp.disziplin === 'slalom' && x.stage.phase === 'PREP', 20000);
  check('Wettkampf startet mit dem Riesenslalom', Boolean(s), JSON.stringify(s?.comp));
  check('„Auf geht\'s!“ mit 4 Leuten', sees(anna, /Auf geht's! Wettkampf \(alle drei\) mit 4 Leuten/), anna.chats.find((c) => /Auf geht/.test(c)));
  check('Startnummer (englisch: Bib number)', await waitFor(() => sees(anna, /Startnummer: \d/) && sees(bert, /Your bib number: \d/), 3000),
    bert.chats.filter((c) => /bib|Bib|number/.test(c)).join(' | '));
  check('Netzwerk: Zustand „running“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'running'), 3000));
  check('Ein Lauf mit vier Bahnen', s?.stage.heat === '1/1' && Object.keys(s?.teams ?? {}).length === 4, JSON.stringify(s?.stage));
  await sleep(1500);
  s = await status();
  check('Alle sitzen im Boot am Start', Object.values(s.teams).every((t) => t.boat && t.passengers === 1) &&
    [anna, bert, carl, dora].every((b) => b.vehicle != null), Object.values(s.teams).map((t) => `${t.t}:${t.passengers}`).join(' '));
  check('Leibchen mit Startnummer an', anna.inventory.slots[6]?.name === 'leather_chestplate', anna.inventory.slots[6]?.name);
  check('Rückhol-Feder in der Schnellleiste', slot(anna, 0)?.name === 'feather' && slot(anna, 8)?.name === 'red_dye');
  const zStart = s.teams.Anna?.z;
  s = await waitStage((x) => x.stage.phase === 'RUN', 12000);
  check('Countdown 3-2-1, dann LOS', Boolean(s) && sawTitle(anna, /^3$/) && sawTitle(anna, /LOS/) && sawTitle(bert, /GO/),
    anna.titles.join(','));
  check('Vor dem LOS bewegt sich kein Boot (Starttor zu)', Math.abs((s?.teams.Anna?.z ?? 0) - zStart) < 0.8, `${zStart} → ${s?.teams.Anna?.z}`);

  // Fahrten: Anna fehlerfrei, Bert verpasst Tor 1, Carl 1 s später, Dora steigt aus und wird zurückgesetzt.
  await cmd('wsadmin fahren Anna ziel', 'wsadmin fahren Bert tor 0 daneben');
  await sleep(1500);
  s = await status();
  check('Tor verpasst: Strafe gezählt', s.teams.Bert?.misses === 1, JSON.stringify(s.teams.Bert));
  check('Hinweis „Gate missed! +3 s“ (englisch)', sawBar(bert, /Gate missed! \+3 s/), bert.bars.slice(-3).join(' | '));
  await cmd('wsadmin fahren Bert ziel', 'wsadmin fahren Carl ziel');
  await cmd('wsadmin fahren Dora tor 0');
  await sleep(1500);
  await sneakOut(dora);
  check('Aussteigen = zurück aufs Eis beim letzten Tor (neues Boot)', await waitFor(async () => {
    const x = await status();
    return dora.vehicle != null && x.teams.Dora?.passengers === 1 && x.teams.Dora?.z > -276 && x.teams.Dora?.z < -260;
  }, 6000), JSON.stringify((await status()).teams.Dora));
  check('Hinweis „Zurück auf die Strecke“', await waitFor(() => sees(dora, /Zurück auf die Strecke/), 2000), dora.chats.slice(-3).join(' | '));
  await sleep(1000);
  await cmd('wsadmin fahren Dora ziel');
  s = await waitStage((x) => x.comp.disziplin !== 'slalom' || x.comp.phase === 'BETWEEN', 60000);
  check('Alle im Ziel, Riesenslalom gewertet', Boolean(s), JSON.stringify(s?.comp));
  check('„Im Ziel!“ mit Zeit', sees(anna, /Im Ziel! Zeit: \d:\d\d,\d\d/) && sees(bert, /Finished! Time: \d:\d\d\.\d\d \(incl\. 3 s penalty\)/),
    `${anna.chats.find((c) => /Im Ziel/.test(c))} / ${bert.chats.find((c) => /Finished/.test(c))}`);
  check('Ergebnisliste im Chat (mit verpasstem Tor)', sees(anna, /Ergebnis Riesenslalom/) && sees(anna, /1 Tor\(e\) verpasst/));
  check('Gold für Anna (fehlerfrei, zuerst los)', sees(anna, /GOLD! Du gewinnst Riesenslalom/) && sawTitle(anna, /Gold/),
    anna.chats.filter((c) => /Gold|Silber|Bronze/.test(c)).join(' | '));
  check('Silber für Carl', sees(carl, /Silber! Platz 2/));
  check('Bronze für Platz 3 (Bert mit Strafe oder Dora nach dem Aussteigen)', sees(bert, /Bronze! 3rd place/) !== sees(dora, /Bronze! Platz 3/),
    `${bert.chats.filter((c) => /3rd/.test(c)).join(' | ')} / ${dora.chats.filter((c) => /Platz 3/.test(c)).join(' | ')}`);
  check('Medaillenspiegel im Chat', sees(anna, /Medaillenspiegel/) && sees(anna, /1\. Anna ●1 ●0 ●0/), anna.chats.filter((c) => /●/.test(c)).join(' | '));
  const st1 = calls().filter((c) => c.t === 'stats');
  check('Netzwerk: Gold + Zeit für Anna', st1.some((c) => c.name === 'Anna' && c.add.gold === 1 && c.add.races === 1) &&
    st1.some((c) => c.name === 'Anna' && c.best.time_slalom_ms > 0 && c.lower === true), JSON.stringify(st1.filter((c) => c.name === 'Anna')));
  check('Zwischen den Disziplinen: „Gleich geht es weiter mit Biathlon“', await waitFor(() => sees(anna, /weiter mit Biathlon/), 4000));

  // ------------------------------------------------------------------ Biathlon
  s = await waitStage((x) => x.comp.disziplin === 'biathlon' && x.stage.phase === 'COUNT', 20000);
  check('Biathlon: alle am Start', Boolean(s) && Object.keys(s.runners).length === 4, JSON.stringify(s?.stage));
  check('Erklärung zum Biathlon (englisch)', sees(bert, /Biathlon: 3 laps/));
  const before = anna.entity.position.clone();
  anna.setControlState('forward', true);
  await sleep(900);
  anna.clearControlStates();
  check('Vor dem Start: das Starttor hält alle zurück', anna.entity.position.x < 102.5,
    `${before} → ${anna.entity.position}`);
  s = await waitStage((x) => x.stage.phase === 'RUN', 10000);
  check('Massenstart: LOS', Boolean(s) && sawTitle(anna, /LOS/));
  // Anna läuft die erste Runde wirklich, die anderen schiebt das Plugin um den Kurs.
  await cmd('wsadmin laufen Bert runde', 'wsadmin laufen Carl runde', 'wsadmin laufen Dora runde');
  const walked = walkPoints(anna, loopPoints(), 70000);
  const bertShoot = shootRound(bert, 'Bert', 3);
  const carlShoot = shootRound(carl, 'Carl', 5);
  const doraShoot = shootRound(dora, 'Dora', 4);
  check('Anna läuft die Runde wirklich (Speed vom Plugin)', await walked);
  let annaAtRange = await waitStage((x) => x.runners.Anna?.state === 'shooting', 15000);
  if (!annaAtRange) {
    await cmd('wsadmin laufen Anna runde');
    annaAtRange = await waitStage((x) => x.runners.Anna?.state === 'shooting', 30000);
  }
  check('Nach Runde 1 an den Schießstand (eigene Bahn)', Boolean(annaAtRange), JSON.stringify((await status()).runners.Anna));
  check('Bogen und fünf Pfeile', slot(anna, 0)?.name === 'bow' && slot(anna, 1)?.name === 'arrow' && slot(anna, 1)?.count === 5,
    `${slot(anna, 0)?.name} ${slot(anna, 1)?.name}×${slot(anna, 1)?.count}`);
  check('Titel „Schießstand“', sawTitle(anna, /Schießstand/));
  const annaShoot = shootRound(anna, 'Anna', 5);
  check('Anna: alle fünf getroffen', await annaShoot && await waitFor(() => sees(anna, /5 von 5 getroffen – fehlerfrei/), 3000),
    anna.chats.slice(-2).join(' | '));
  const scheiben = String(await cmd(inWorld('execute if block 77 102 38 minecraft:white_concrete')));
  check('Getroffene Scheiben klappen weiß um', /passed/i.test(scheiben), scheiben.trim());
  check('Bert: 3 Treffer, 2 Fehler → 2 Strafrunden', await bertShoot && sees(bert, /Hit 3 of 5 – 2 penalty loop\(s\)/),
    bert.chats.slice(-2).join(' | '));
  check('Treffer- und Fehler-Hinweise', sawBar(bert, /Hit!/) && sawBar(bert, /Miss!/));
  check('Carl fehlerfrei, Dora 4 von 5', await carlShoot && await doraShoot && sees(carl, /5 von 5/) && sees(dora, /4 von 5/));
  s = await status();
  check('Bert ist in der Strafrunde', s.runners.Bert?.state === 'penalty' && s.runners.Bert?.pen === 2, JSON.stringify(s.runners.Bert));
  const others = [penaltiesThenLap('Anna'), penaltiesThenLap('Carl'), penaltiesThenLap('Dora')];
  await cmd('wsadmin laufen Bert runde');
  await sleep(4000);
  s = await status();
  check('Strafrunde zuerst: die Loipe zählt nicht', s.runners.Bert?.lap === 1 && s.runners.Bert?.state === 'penalty', JSON.stringify(s.runners.Bert));
  await cmd('wsadmin laufen Bert strafrunde');
  await waitFor(async () => (await status()).runners.Bert?.pen === 1, 12000);
  await cmd('wsadmin laufen Bert strafrunde');
  check('Zwei Strafrunden geschafft, weiter auf der Loipe', await waitFor(async () => (await status()).runners.Bert?.state === 'running', 12000) &&
    sees(bert, /Penalty loops done/), JSON.stringify((await status()).runners.Bert));
  await cmd('wsadmin laufen Bert runde');
  await Promise.all(others);
  // Runde 2 → zweites Schießen → Strafrunden → Runde 3 ins Ziel
  await Promise.all([[anna, 'Anna', 5], [bert, 'Bert', 5], [carl, 'Carl', 4], [dora, 'Dora', 5]].map(async ([bot, name, hits]) => {
    await shootRound(bot, name, hits);
    await penaltiesThenLap(name);
  }));
  s = await waitStage((x) => x.comp.disziplin !== 'biathlon' || x.comp.phase === 'BETWEEN', 150000);
  check('Biathlon: alle im Ziel und gewertet', Boolean(s) && sees(anna, /Ergebnis Biathlon/), JSON.stringify(s?.comp));
  check('Ergebnis mit Treffern', sees(anna, /\(10\/10 Treffer\)/) && sees(bert, /\(8\/10 hits\)/), anna.chats.filter((c) => /Treffer/.test(c)).join(' | '));
  const st2 = calls().filter((c) => c.t === 'stats' && 'biathlon_hits' in (c.add ?? {}));
  check('Netzwerk: biathlon_hits', st2.some((c) => c.name === 'Bert' && c.add.biathlon_hits === 8) &&
    st2.some((c) => c.name === 'Anna' && c.add.biathlon_hits === 10), JSON.stringify(st2.map((c) => [c.name, c.add.biathlon_hits])));
  check('Netzwerk: Biathlon-Zeit (kleiner ist besser)', calls().some((c) => c.t === 'stats' && c.best?.time_biathlon_ms > 0 && c.lower));

  // ------------------------------------------------------------------ Bob mit Zweierbob
  s = await waitStage((x) => x.comp.disziplin === 'bob' && x.stage.phase === 'PREP', 20000);
  check('Bobbahn: Anna und Carl (Party) im Zweierbob', Boolean(s) && s.teams['Anna+Carl'] && s.teams.Bert && s.teams.Dora,
    Object.keys(s?.teams ?? {}).join(' '));
  check('Hinweis Zweierbob: Anna lenkt', sees(carl, /Zweierbob.*Anna lenkt/));
  await sleep(1500);
  s = await status();
  check('Zu zweit in einem Bob', s.teams['Anna+Carl']?.passengers === 2 && anna.vehicle != null && carl.vehicle != null &&
    anna.vehicle?.id === carl.vehicle?.id, JSON.stringify(s.teams['Anna+Carl']));
  s = await waitStage((x) => x.stage.phase === 'RUN', 12000);
  await cmd('wsadmin fahren Anna ziel');
  await sleep(1000);
  await cmd('wsadmin fahren Bert ziel', 'wsadmin fahren Dora weiter 60');
  await sleep(3500);
  s = await status();
  check('Bob fährt durch die Kehren (Kontrollpunkte)', s.teams.Dora?.cp >= 2, JSON.stringify(s.teams.Dora));
  check('Anzeige „Kehre n/12“', sawBar(dora, /Kehre \d+\/12/), dora.bars.slice(-1)[0]);
  const cpBefore = s.teams.Dora?.cp;
  await sneakOut(dora);
  check('Aussteigen im Kanal = zurück zum letzten Kontrollpunkt', await waitFor(async () => {
    const x = await status();
    return dora.vehicle != null && x.teams.Dora?.passengers === 1 && x.teams.Dora?.cp === cpBefore;
  }, 6000), JSON.stringify((await status()).teams.Dora));
  await sleep(800);
  await cmd('wsadmin fahren Dora ziel');
  s = await waitStage((x) => x.comp.phase === 'CEREMONY' || !x.top.laeuft, 60000);
  check('Bob gewertet: Gold für den Zweierbob', sees(anna, /GOLD! Du gewinnst Bobbahn/) && sees(carl, /GOLD! Du gewinnst Bobbahn/),
    anna.chats.filter((c) => /Bob/.test(c)).join(' | '));
  check('Netzwerk: Bob-Zeit für beide im Zweierbob', ['Anna', 'Carl'].every((n) => calls().some((c) => c.t === 'stats' &&
    c.name === n && c.best?.time_bob_ms > 0)));

  // ------------------------------------------------------------------ Siegerehrung
  check('Siegerehrung: Titel', await waitFor(() => sawTitle(anna, /Siegerehrung/) && sawTitle(bert, /Award ceremony/), 6000));
  check('Gesamtsieg für Anna', sees(anna, /Gesamtsieg für Anna/), anna.chats.find((c) => /Gesamtsieg/.test(c)));
  const onPodium = await waitFor(() => anna.entity.position.y > FLOOR + 3.5 && Math.abs(anna.entity.position.z + 8.5) < 1, 4000);
  check('Anna steht ganz oben auf dem Treppchen', onPodium, JSON.stringify(anna.entity.position));
  check('Zurück ins Dorf nach der Siegerehrung', await waitFor(() => near(anna.entity.position, SPAWN, 3) && slot(anna, 0)?.name === 'compass', 15000),
    JSON.stringify(anna.entity.position));
  const ends = calls().filter((c) => c.t === 'stats' && c.add?.events === 1);
  check('Netzwerk: events für alle, wins für Anna', ends.length === 4 && ends.some((c) => c.name === 'Anna' && c.add.wins === 1) &&
    !ends.some((c) => c.name !== 'Anna' && c.add.wins), JSON.stringify(ends.map((c) => [c.name, c.add])));
  check('Netzwerk: best_points (größer ist besser)', ends.some((c) => c.best?.best_points > 0 && c.lower === false));
  check('Netzwerk: Zustand wieder „waiting“', await waitFor(() => {
    const st = calls().filter((c) => c.t === 'state');
    return st[st.length - 1].state === 'waiting';
  }, 4000));
  const af = readFile(playerFile('Anna'));
  check('Spielerdatei: Medaillen und Bestzeiten', /gold: [2-9]/.test(af) && /slalom: \d+/.test(af) && /gesamtsiege: 1/.test(af), af.replace(/\n/g, ' '));
  check('Bestenliste gespeichert', /zeiten:/.test(readFile(path.join(dataDir, 'bestenliste.yml'))));
  clear(anna, bert);
  anna.chat('/stats');
  bert.chat('/top slalom');
  await sleep(800);
  check('/stats zeigt Medaillen', sees(anna, /Gold · .*Silber/) && sees(anna, /Gesamtsiege: 1/), anna.chats.join(' | '));
  check('/top slalom (englisch)', sees(bert, /Best times Giant Slalom/) && sees(bert, /1\. Anna/), bert.chats.join(' | '));

  // ------------------------------------------------------------------ Zuschauen + Wettkampf 2 (Riesenslalom, zwei Läufe)
  const emil = await join('Emil');
  for (const b of [anna, bert, carl, dora, emil]) b.chat('/abstimmen slalom');
  await sleep(800);
  check('Abstimmung für den Riesenslalom', sees(emil, /Deine Stimme: Riesenslalom/));
  // Emil bleibt nicht bereit: er schaut erst zu (mindestens-spieler 3 → Countdown läuft trotzdem nicht, solange
  // nicht alle bereit sind; darum starten wir per Verwaltung mit allen fünf).
  await cmd('wsadmin start slalom');
  s = await waitStage((x) => x.comp.wettkampf === 'slalom' && x.stage.phase === 'PREP', 20000);
  check('Zweiter Wettkampf: Riesenslalom mit fünf Leuten in zwei Läufen', Boolean(s) && s.stage.heat === '1/2' &&
    Object.keys(s.teams).length === 5, JSON.stringify(s?.stage));
  const waiting = Object.values(s?.teams ?? {}).filter((t) => t.heat === 2).map((t) => t.t);
  const second = waiting.map((n) => bots[n]);
  check('Wer im zweiten Lauf fährt, wartet auf der Tribüne', second.length === 2 && second.every((b) => sees(b, /zweiten|Lauf 2|heat 2/i) ||
    sees(b, /im Lauf 2|in heat 2/)), waiting.join(','));
  s = await waitStage((x) => x.stage.phase === 'RUN', 12000);
  const first = Object.values(s.teams).filter((t) => t.heat === 1).map((t) => t.t);
  for (const n of first) await cmd(`wsadmin fahren ${n} ziel`);
  s = await waitStage((x) => x.stage.heat === '2/2' && x.stage.phase === 'RUN', 40000);
  check('Lauf 2 startet nach Lauf 1', Boolean(s), JSON.stringify(s?.stage));
  const giver = waiting[1];
  await cmd(`wsadmin fahren ${waiting[0]} ziel`);
  await useItem(bots[giver], 8);
  check('Aufgeben: nicht im Ziel, zurück auf die Tribüne', await waitFor(async () => (await status()).teams[giver]?.state === 'dnf', 4000));
  s = await waitStage((x) => x.comp.phase === 'CEREMONY' || !x.top.laeuft, 40000);
  check('Wertung: wer aufgibt, ist „nicht im Ziel“', await waitFor(() => sees(carl, new RegExp(`${giver} – (nicht im Ziel|did not finish)`)), 3000),
    carl.chats.slice(-8).join(' | '));
  check('Siegerehrung auch für eine Disziplin allein', await waitFor(() => sawTitle(emil, /Siegerehrung/), 6000));
  check('Danach wieder alle im Dorf', await waitFor(() => [anna, bert, carl, dora, emil].every((b) => near(b.entity.position, SPAWN, 4)), 15000));

  // Zuschauen während eines Wettkampfs (Emil schaut, die anderen fahren Bob)
  await cmd('wsadmin start bob');
  await waitStage((x) => x.comp.wettkampf === 'bob', 15000);
  await cmd('wsadmin stopp');
  await waitFor(async () => !(await status()).top.laeuft, 5000);
  await sleep(500);
  for (const b of [anna, bert, carl, dora]) b.chat('/abstimmen bob');
  await sleep(500);
  // Emil ist nicht bereit und kommt so nicht mit: alle anderen bereit
  for (const b of [anna, bert, carl, dora]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
    await sleep(200);
  }
  await waitStage((x) => x.comp.wettkampf === 'bob', 25000);
  s = await status();
  const emilIn = Object.keys(s.teams).some((k) => k.includes('Emil'));
  if (emilIn) {
    check('Zuschauen: (Emil fährt mit, kein Zuschauer-Test möglich)', true);
  } else {
    win = await openWithItem(emil, 0);
    check('Kompass bietet „Zuschauen“ an', plain(win?.slots[22]?.customName).includes('Zuschauen'), plain(win?.slots[22]?.customName));
    await emil.clickWindow(22, 0, 0);
    await sleep(800);
    check('Zuschauen: auf der Tribüne der Bobbahn', sees(emil, /Du schaust zu: Bobbahn/) && Math.abs(emil.entity.position.x - 120.5) < 3,
      JSON.stringify(emil.entity.position));
    await useItem(emil, 8);
    check('Zuschauen beenden: zurück ins Dorf', await waitFor(() => near(emil.entity.position, SPAWN, 3), 3000));
  }
  await cmd('wsadmin stopp');
  check('Abbruch per Verwaltung: alle zurück ins Dorf', await waitFor(() => [anna, bert, carl, dora].every((b) => near(b.entity.position, SPAWN, 4)), 6000));

  // ------------------------------------------------------------------ Lobby-Knopf
  emil.setQuickBarSlot(8);
  await sleep(250);
  emil.activateItem();
  check('Bett schickt in die Lobby (Netzwerk)', await waitFor(() => calls().some((c) => c.t === 'to-lobby' && c.name === 'Emil'), 3000));

  for (const b of Object.values(bots)) b.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} grün`);
  if (failed.length) {
    console.log('Fehlgeschlagen:');
    failed.forEach((f) => console.log(`  ✗ ${f.name}`));
  }
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  for (const b of Object.values(bots)) b.quit();
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} grün (abgebrochen)`);
  process.exit(1);
});
