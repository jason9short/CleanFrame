/* Android TV application controls through the CleanFrameAndroid bridge (android/app/.../FrameBridge.java). */
(function () {
  'use strict';
  /* Home defaults still carry Tizen IDs; map them to a brand, then to the Android package. */
  var TIZEN = {
    'IkWsFHtOY9.Stremio': 'stremio', 'org.tizen.netflix-app': 'netflix', 'MCmYXNxgcu.DisneyPlus': 'disneyplus',
    '9Ur5IzDKqV.TizenYouTube': 'youtube', 'com.samsung.tv.aria-video': 'appletv', 'org.tizen.primevideo': 'primevideo',
    'mWK87Xghap.ABCiview': 'abciview', 'OyE7OuxlUi.SBSOnDemand': 'sbs', 'org.tizen.apple.apple-music': 'applemusic'
  };
  var PRIMARY = ['stremio', 'netflix', 'disneyplus', 'youtube', 'appletv', 'primevideo', 'abciview', 'sbs', 'applemusic'];
  var BRANDS = {
    stremio: [['com.stremio.one'], /^stremio\b/i],
    netflix: [['com.netflix.ninja'], /^netflix$/i],
    disneyplus: [['com.disney.disneyplus'], /^disney\s*\+?$/i],
    youtube: [['com.google.android.youtube.tv'], /^youtube$/i],
    appletv: [['com.apple.atve.androidtv.appletv'], /^apple tv$/i],
    primevideo: [['com.amazon.amazonvideo.livingroom'], /prime video/i],
    abciview: [['au.net.abc.iview'], /abc iview/i],
    sbs: [['com.sbs.ondemand.tv', 'com.sbs.ondemand.android'], /sbs on demand/i],
    applemusic: [['com.apple.android.music'], /^apple music$/i],
    spotify: [['com.spotify.tv.android'], /^spotify\b/i],
    hbomax: [['com.wbd.stream', 'com.hbo.hbonow'], /^(hbo )?max$/i],
    tubi: [['com.tubitv'], /^tubi\b/i]
  };
  var resolved = {};
  function bridge() { return window.CleanFrameAndroid; }
  function unavailable(done) {
    done({ message: 'Open CleanFrame on the TV to use this control.', detail: 'The web preview cannot control the TV.' });
  }
  function result(done, err) {
    setTimeout(function () { done(err ? { message: err, detail: 'Try again, or press the remote’s Home button to return to CleanFrame.' } : null); }, 0);
  }
  function brandOf(pkg, name) {
    var b;
    for (b in BRANDS) if (BRANDS[b][0].indexOf(pkg) >= 0) return b;
    for (b in BRANDS) if (BRANDS[b][1].test(name)) return b;
    return '';
  }
  function read() {
    var list = JSON.parse(bridge().apps()), out = [];
    list.forEach(function (a) {
      var brand = brandOf(a.id, a.name), primary = PRIMARY.indexOf(brand);
      if (brand && !resolved[brand]) resolved[brand] = a.id;
      out.push({ appId: a.id, name: a.name, brand: brand, icon: '/cf-icon/' + a.id + '.png', rank: primary >= 0 ? primary : (brand ? 20 : 30) });
    });
    out.sort(function (a, b) { return a.rank - b.rank || a.name.toLowerCase().localeCompare(b.name.toLowerCase()); });
    return out;
  }
  function packageFor(id) {
    var brand = TIZEN[id];
    if (!brand) return id;
    if (!resolved[brand]) { try { read(); } catch (e) { /* fall through to the known package */ } }
    return resolved[brand] || BRANDS[brand][0][0];
  }
  function launchApp(id, done) {
    if (!bridge()) { unavailable(done); return; }
    result(done, bridge().launch(packageFor(id)));
  }
  function launch(brand, done) {
    if (!BRANDS[brand]) { done({message: 'This app has no launch target.'}); return; }
    if (!bridge()) { unavailable(done); return; }
    if (!resolved[brand]) { try { read(); } catch (e) { /* use the known package */ } }
    result(done, bridge().launch(resolved[brand] || BRANDS[brand][0][0]));
  }
  function call(method) {
    return function (done) {
      if (!bridge()) { unavailable(done); return; }
      result(done, bridge()[method]());
    };
  }
  function apps(done) {
    if (!bridge()) { unavailable(done); return; }
    var list;
    try { list = read(); } catch (e) { result(done, 'Could not read installed apps.'); return; }
    setTimeout(function () { done(null, list); }, 0);
  }
  window.FrameDevice = {
    launch: launch, launchApp: launchApp, art: call('screensaver'), home: call('home'), settings: call('settings'), apps: apps,
    labels: { art: 'Screensaver', vendor: 'Android TV', home: 'Android TV Home', homeNote: 'Open the stock Android TV home screen' }
  };
}());
