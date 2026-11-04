const $ = (s) => document.querySelector(s);

const MAX_SIZE = 200 * 1024 * 1024;
const ALLOWED = ["jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "webm"];

const state = {
  auth: sessionStorage.getItem("auth"),
  user: sessionStorage.getItem("user"),
  file: null,
};

/* ---------- UI helpers ---------- */
function toast(msg, type = "") {
  const t = document.createElement("div");
  t.className = "toast " + type;
  t.textContent = msg;
  $("#toasts").append(t);
  setTimeout(() => t.remove(), 4000);
}

function ext(name) {
  const i = name.lastIndexOf(".");
  return i < 0 ? "" : name.slice(i + 1).toLowerCase();
}

function isVideo(url) {
  return /\.(mp4|mov|webm)(\?.*)?$/i.test(url);
}

/* ---------- Auth ---------- */
function renderAuth() {
  const logged = !!state.auth;
  $("#loginForm").classList.toggle("hidden", logged);
  $("#authInfo").classList.toggle("hidden", !logged);
  $("#userLabel").textContent = state.user || "";
}

function login(user, pass) {
  state.auth = btoa(user + ":" + pass);
  state.user = user;
  sessionStorage.setItem("auth", state.auth);
  sessionStorage.setItem("user", user);
  renderAuth();
}

function logout() {
  state.auth = null;
  state.user = null;
  sessionStorage.removeItem("auth");
  sessionStorage.removeItem("user");
  renderAuth();
}

function authHeaders() {
  return {
    Authorization: "Basic " + state.auth,
    "X-Requested-With": "XMLHttpRequest", // fără popup nativ la 401
  };
}

$("#loginForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const user = $("#user").value.trim();
  const pass = $("#pass").value;
  if (!user || !pass) return toast("Completează utilizatorul și parola", "error");
  login(user, pass);
  $("#pass").value = "";
  toast("Conectat ca " + user, "ok");
});

$("#logoutBtn").addEventListener("click", logout);

/* ---------- File selection ---------- */
function setFile(file) {
  if (!file) return clearFile();

  if (!ALLOWED.includes(ext(file.name))) {
    return toast("Extensie nepermisă: ." + ext(file.name), "error");
  }
  if (file.size > MAX_SIZE) {
    return toast("Fișier prea mare (max 200 MB)", "error");
  }

  state.file = file;
  const url = URL.createObjectURL(file);
  const el = document.createElement(file.type.startsWith("video/") ? "video" : "img");
  el.src = url;
  if (el.tagName === "VIDEO") el.controls = true;

  const name = document.createElement("div");
  name.className = "fname";
  name.textContent = file.name + " (" + (file.size / 1024 / 1024).toFixed(1) + " MB)";

  $("#preview").replaceChildren(el, name);
  $("#preview").classList.remove("hidden");
  $("#dropHint").classList.add("hidden");
  $("#clearFile").classList.remove("hidden");
}

function clearFile() {
  state.file = null;
  $("#file").value = "";
  $("#preview").replaceChildren();
  $("#preview").classList.add("hidden");
  $("#dropHint").classList.remove("hidden");
  $("#clearFile").classList.add("hidden");
}

const dz = $("#dropzone");
dz.addEventListener("click", (e) => {
  if (e.target.closest("#preview")) return; // nu deschide dialogul când folosești player-ul
  $("#file").click();
});
dz.addEventListener("keydown", (e) => {
  if (e.key === "Enter" || e.key === " ") { e.preventDefault(); $("#file").click(); }
});
$("#file").addEventListener("change", (e) => setFile(e.target.files[0]));
$("#clearFile").addEventListener("click", clearFile);

["dragenter", "dragover"].forEach((ev) =>
  dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("drag"); })
);
["dragleave", "drop"].forEach((ev) =>
  dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.remove("drag"); })
);
dz.addEventListener("drop", (e) => setFile(e.dataTransfer.files[0]));

/* ---------- API ---------- */
function uploadFile(file, onProgress) {
  return new Promise((resolve, reject) => {
    const fd = new FormData();
    fd.append("file", file);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/media");
    for (const [k, v] of Object.entries(authHeaders())) xhr.setRequestHeader(k, v);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(e.loaded / e.total);
    };
    xhr.onload = () => {
      if (xhr.status === 201) return resolve(JSON.parse(xhr.responseText).mediaUrl);
      if (xhr.status === 401) {
        logout();
        return reject(new Error("Neautorizat: utilizator sau parolă greșite"));
      }
      let msg;
      try { msg = JSON.parse(xhr.responseText).error; } catch { /* ignore */ }
      reject(new Error(msg || "Upload eșuat (" + xhr.status + ")"));
    };
    xhr.onerror = () => reject(new Error("Conexiune întreruptă (verifică login-ul)"));
    xhr.send(fd);
  });
}

async function createPostRequest(payload) {
  const res = await fetch("/api/posts", {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(payload),
  });
  if (res.status === 401) {
    logout();
    throw new Error("Neautorizat: utilizator sau parolă greșite");
  }
  if (!res.ok) throw new Error("Crearea postării a eșuat (" + res.status + ")");
  return res.json();
}

/* ---------- Submit ---------- */
function setProgress(p) {
  $("#progressWrap").classList.toggle("hidden", p === null);
  $("#progressBar").style.width = p === null ? "0" : Math.round(p * 100) + "%";
}

$("#submit").addEventListener("click", async () => {
  if (!state.auth) {
    $("#pass").focus();
    return toast("Fă login mai întâi (sus, în bara de navigare)", "error");
  }
  const title = $("#title").value.trim();
  if (!title) return toast("Titlul este obligatoriu", "error");

  const btn = $("#submit");
  btn.disabled = true;
  btn.textContent = "Se publică...";

  try {
    let imageUrl = $("#url").value.trim() || null;
    if (state.file) {
      setProgress(0);
      imageUrl = await uploadFile(state.file, setProgress);
    }

    await createPostRequest({
      title,
      content: $("#content").value,
      imageUrl,
    });

    $("#title").value = "";
    $("#content").value = "";
    $("#url").value = "";
    clearFile();
    toast("Postare publicată", "ok");
    loadPosts();
  } catch (e) {
    toast(e.message, "error");
  } finally {
    setProgress(null);
    btn.disabled = false;
    btn.textContent = "Publică";
  }
});

/* ---------- Feed ---------- */
function renderPost(p) {
  const card = document.createElement("article");
  card.className = "card post";

  if (p.imageUrl) {
    const media = document.createElement(isVideo(p.imageUrl) ? "video" : "img");
    media.src = p.imageUrl;
    if (media.tagName === "VIDEO") { media.controls = true; media.preload = "metadata"; }
    else { media.loading = "lazy"; media.alt = p.title || ""; }
    card.append(media);
  }

  const body = document.createElement("div");
  body.className = "post-body";
  const h = document.createElement("h3");
  h.textContent = p.title || "";
  body.append(h);
  if (p.content) {
    const c = document.createElement("p");
    c.textContent = p.content;
    body.append(c);
  }
  card.append(body);
  return card;
}

async function loadPosts() {
  const box = $("#posts");
  try {
    const res = await fetch("/api/posts");
    if (!res.ok) throw new Error();
    const posts = (await res.json()).reverse();
    box.replaceChildren();
    if (posts.length === 0) {
      const empty = document.createElement("div");
      empty.className = "empty";
      empty.textContent = "Nu există postări încă.";
      box.append(empty);
      return;
    }
    posts.forEach((p) => box.append(renderPost(p)));
  } catch {
    box.textContent = "Nu s-au putut încărca postările.";
  }
}

renderAuth();
loadPosts();