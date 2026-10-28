// Sends Chilavu alerts through Firebase when the phone app is closed.
// Secrets (set in Supabase, never in the Android app):
//   FIREBASE_SERVICE_ACCOUNT  — Firebase service-account JSON
//   PUSH_WEBHOOK_SECRET       — shared with the database trigger
//   SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase.

import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

type Row = Record<string, unknown>;

type PushBody = {
  action?: string;
  table?: string;
  op?: string;
  actor?: string | null;
  new?: Row | null;
  old?: Row | null;
};

type Alert = { title: string; body: string; tag: string; userIds: string[] };

const PROJECT_ID_FALLBACK = "trip-budget-tracker-dd5f0";

let cachedToken: { value: string; exp: number } | null = null;

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("ok", { status: 200 });
  const secret = Deno.env.get("PUSH_WEBHOOK_SECRET") ?? "";
  const sent = req.headers.get("x-push-secret") ?? "";
  if (!secret || sent !== secret) return new Response("unauthorized", { status: 401 });

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  );
  const body = (await req.json().catch(() => ({}))) as PushBody;
  try {
    if (body.action === "weather") {
      await checkWeather(supabase);
    } else if (body.action === "row") {
      await handleRow(supabase, body);
    }
  } catch (err) {
    console.error("push failed", err);
  }
  return new Response("ok", { status: 200 });
});

async function handleRow(supabase: SupabaseClient, body: PushBody): Promise<void> {
  const table = body.table ?? "";
  const op = body.op ?? "";
  const actor = body.actor ?? null;
  const record = op === "DELETE" ? body.old ?? {} : body.new ?? {};
  const previous = body.old ?? {};
  const tripId = text(record.trip_id) || text(previous.trip_id);
  if (!tripId) return;

  const alerts = await alertsFor(supabase, table, op, actor, tripId, record, previous);
  for (const alert of alerts) {
    await deliver(supabase, alert);
  }
}

async function alertsFor(
  supabase: SupabaseClient,
  table: string,
  op: string,
  actor: string | null,
  tripId: string,
  record: Row,
  previous: Row,
): Promise<Alert[]> {
  const trip = await tripRow(supabase, tripId);
  if (!trip) return [];
  const tripName = text(trip.name) || "Your trip";
  const members = await memberIds(supabase, tripId);

  if (table === "trip_announcements" && op === "INSERT") {
    const title = text(record.title);
    const message = text(record.body);
    if (!title || !message) return [];
    return [{
      title,
      body: message,
      tag: `announce:${tripId}:${title.slice(0, 40)}`,
      userIds: members,
    }];
  }

  if (table === "trip_access" && op === "INSERT" && text(record.role) !== "owner") {
    const joined = text(record.user_id);
    const who = text(record.display_name);
    const owners = await memberIds(supabase, tripId, "owner");
    return [{
      title: "Friend joined",
      body: who ? `${who} · ${tripName}` : tripName,
      tag: `join:${tripId}:${joined}`,
      userIds: owners.filter((id) => id !== joined),
    }];
  }

  const others = members.filter((id) => id !== actor);
  const alerts: Alert[] = [];

  if (table === "expenses" && (op === "INSERT" || op === "UPDATE" || op === "DELETE")) {
    const title = text(record.title) || text(previous.title);
    const amount = formatInr(number(record.amount_paise ?? previous.amount_paise));
    const detail = [tripName, title, amount].filter(Boolean).join(" · ");
    const headline = op === "INSERT" ? "New expense" : op === "DELETE" ? "Expense removed" : "Expense updated";
    alerts.push({
      title: headline,
      body: detail,
      tag: `expenses:${text(record.id) || text(previous.id)}:${op}`,
      userIds: others,
    });
  }

  if (table === "contributions" && (op === "INSERT" || op === "UPDATE" || op === "DELETE")) {
    const note = text(record.note) || text(previous.note);
    const amount = formatInr(number(record.amount_paise ?? previous.amount_paise));
    const detail = [tripName, note, amount].filter(Boolean).join(" · ");
    const headline = op === "INSERT"
      ? "New contribution"
      : op === "DELETE"
      ? "Contribution removed"
      : "Contribution updated";
    alerts.push({
      title: headline,
      body: detail,
      tag: `contributions:${text(record.id) || text(previous.id)}:${op}`,
      userIds: others,
    });
  }

  if (table === "members" && op === "INSERT") {
    const who = text(record.name);
    alerts.push({
      title: "New member",
      body: who ? `${who} joined ${tripName}` : tripName,
      tag: `members:${text(record.id)}:INSERT`,
      userIds: others,
    });
  }

  if (table === "route_stops" && op === "UPDATE" && text(record.status) === "REACHED" && trip.route_tracking_active === true) {
    const stop = text(record.title) || "Stop";
    alerts.push({
      title: "Stop reached",
      body: `${tripName}: ${stop}`,
      tag: `route_stops:${text(record.id)}:UPDATE`,
      userIds: others,
    });
  }

  alerts.push(...await budgetAlerts(supabase, table, op, tripId, tripName, record, previous, others));
  return alerts.filter((alert) => alert.userIds.length > 0 && alert.title && alert.body);
}

async function budgetAlerts(
  supabase: SupabaseClient,
  table: string,
  op: string,
  tripId: string,
  tripName: string,
  record: Row,
  previous: Row,
  userIds: string[],
): Promise<Alert[]> {
  if (table !== "expenses" && table !== "contributions" && table !== "members") return [];
  const trip = await tripRow(supabase, tripId);
  if (!trip) return [];
  const planned = number(trip.planned_budget_paise);
  const { data: expenses } = await supabase.from("expenses").select("id, amount_paise").eq("trip_id", tripId);
  const { data: members } = await supabase
    .from("members")
    .select("id, name, expected_contribution_paise")
    .eq("trip_id", tripId);
  const { data: contributions } = await supabase
    .from("contributions")
    .select("id, member_id, amount_paise")
    .eq("trip_id", tripId);
  const expenseRows = (expenses ?? []) as Row[];
  const memberRows = (members ?? []) as Row[];
  const contributionRows = (contributions ?? []) as Row[];
  const alerts: Alert[] = [];

  if (table === "expenses") {
    const after = sum(expenseRows, "amount_paise");
    let before = after;
    const next = number(record.amount_paise);
    const old = number(previous.amount_paise);
    if (op === "INSERT") before = after - next;
    else if (op === "DELETE") before = after + old;
    else if (op === "UPDATE") before = after - next + old;
    if (planned - before >= 0 && planned - after < 0) {
      alerts.push({
        title: "Over budget",
        body: `${tripName} is ${formatInr(after - planned)} over plan`,
        tag: `over:${tripId}`,
        userIds,
      });
    }
  }

  if (table === "contributions" || table === "members") {
    const after = owedByMember(memberRows, contributionRows);
    const beforeMembers = memberRows.map((row) => ({ ...row }));
    const beforeContribs = contributionRows.map((row) => ({ ...row }));
    const id = text(record.id) || text(previous.id);
    if (table === "contributions" && op === "INSERT") {
      removeId(beforeContribs, id);
    } else if (table === "contributions" && op === "DELETE") {
      beforeContribs.push(previous);
    } else if (table === "contributions" && op === "UPDATE") {
      replaceId(beforeContribs, id, previous);
    } else if (table === "members" && op === "INSERT") {
      removeId(beforeMembers, id);
    } else if (table === "members" && op === "UPDATE") {
      replaceId(beforeMembers, id, previous);
    } else if (table === "members" && op === "DELETE") {
      beforeMembers.push(previous);
    }
    const before = owedByMember(beforeMembers, beforeContribs);
    const lines: string[] = [];
    for (const [memberId, now] of after) {
      const was = before.get(memberId)?.owed ?? 0;
      if (now.owed > 0 && now.owed > was) lines.push(`${now.name} owes ${formatInr(now.owed)}`);
    }
    if (lines.length > 0) {
      alerts.push({
        title: "Money still owed",
        body: `${tripName}: ${lines.join(", ")}`,
        tag: `owed:${tripId}`,
        userIds,
      });
    }
  }
  return alerts;
}

async function checkWeather(supabase: SupabaseClient): Promise<void> {
  const { data: trips } = await supabase
    .from("trips")
    .select("id, name, place_lat, place_lng")
    .not("place_lat", "is", null)
    .not("place_lng", "is", null);
  for (const trip of trips ?? []) {
    const tripId = text(trip.id);
    const lat = number(trip.place_lat);
    const lng = number(trip.place_lng);
    if (!tripId) continue;
    const current = await fetchWeather(lat, lng);
    if (!current) continue;
    const { data: seen } = await supabase
      .from("trip_weather_state")
      .select("kind, label")
      .eq("trip_id", tripId)
      .maybeSingle();
    await supabase.from("trip_weather_state").upsert({
      trip_id: tripId,
      kind: current.kind,
      label: current.label,
    });
    if (!seen || seen.kind === current.kind) continue;
    const from = text(seen?.label) || text(seen?.kind);
    await deliver(supabase, {
      title: "Weather changed",
      body: `${text(trip.name) || "Your trip"}: ${from} to ${current.label}`,
      tag: `weather:${tripId}`,
      userIds: await memberIds(supabase, tripId),
    });
  }
}

async function fetchWeather(lat: number, lng: number): Promise<{ kind: string; label: string } | null> {
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current=weather_code`;
  const response = await fetch(url);
  if (!response.ok) return null;
  const json = await response.json();
  const code = Number(json?.current?.weather_code);
  if (!Number.isFinite(code)) return null;
  const kind = weatherKind(code);
  return { kind, label: weatherLabel(kind, code) };
}

function weatherKind(code: number): string {
  if (code === 0) return "SUNNY";
  if (code === 1 || code === 2 || code === 3) return "CLOUDY";
  if (code === 45 || code === 48) return "FOG";
  if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "RAIN";
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "SNOW";
  if (code >= 95 && code <= 99) return "STORM";
  return "UNKNOWN";
}

function weatherLabel(kind: string, code: number): string {
  if (kind === "SUNNY") return "Sunny";
  if (kind === "CLOUDY") return code === 1 ? "Partly cloudy" : "Cloudy";
  if (kind === "OVERCAST") return "Overcast";
  if (kind === "RAIN") {
    if ([51, 56, 61, 66, 80].includes(code)) return "Light rain";
    if ([55, 57, 65, 67, 82].includes(code)) return "Heavy rain";
    return "Rain";
  }
  if (kind === "STORM") return "Storm";
  if (kind === "SNOW") return "Snow";
  if (kind === "FOG") return "Fog";
  return "Weather";
}

async function deliver(supabase: SupabaseClient, alert: Alert): Promise<void> {
  const ids = [...new Set(alert.userIds)].filter(Boolean);
  if (ids.length === 0) return;
  const { data: rows } = await supabase.from("device_tokens").select("token").in("user_id", ids);
  const tokens = (rows ?? []).map((row) => text(row.token)).filter(Boolean);
  const access = await accessToken();
  const projectId = serviceAccount().project_id || PROJECT_ID_FALLBACK;
  await Promise.all(tokens.map((token) => sendOne(supabase, projectId, access, token, alert)));
}

async function sendOne(
  supabase: SupabaseClient,
  projectId: string,
  access: string,
  token: string,
  alert: Alert,
): Promise<void> {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${access}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: alert.title, body: alert.body },
        data: { title: alert.title, body: alert.body, tag: alert.tag },
        android: {
          priority: "HIGH",
          notification: {
            channel_id: "chilavu_messages",
            tag: alert.tag,
            notification_priority: "PRIORITY_HIGH",
          },
        },
      },
    }),
  });
  if (response.status === 404) {
    await supabase.from("device_tokens").delete().eq("token", token);
  } else if (!response.ok) {
    console.error("fcm", response.status, await response.text());
  }
}

async function accessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.exp - 60 > now) return cachedToken.value;
  const account = serviceAccount();
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64Url(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = `${header}.${claim}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)),
  );
  const jwt = `${unsigned}.${base64UrlBytes(signature)}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!response.ok) throw new Error(`google token ${response.status}`);
  const json = await response.json();
  cachedToken = { value: json.access_token, exp: now + Number(json.expires_in ?? 3600) };
  return cachedToken.value;
}

function serviceAccount(): { client_email: string; private_key: string; project_id?: string } {
  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "";
  const parsed = JSON.parse(raw);
  if (!parsed.client_email || !parsed.private_key) throw new Error("Firebase service account is incomplete");
  return parsed;
}

function pemToDer(pem: string): ArrayBuffer {
  const b64 = pem.replace(/-----BEGIN PRIVATE KEY-----/g, "").replace(/-----END PRIVATE KEY-----/g, "").replace(/\s+/g, "");
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function base64Url(value: string): string {
  return base64UrlBytes(new TextEncoder().encode(value));
}

function base64UrlBytes(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

async function tripRow(supabase: SupabaseClient, tripId: string): Promise<Row | null> {
  const { data } = await supabase
    .from("trips")
    .select("id, name, planned_budget_paise, route_tracking_active, owner_id")
    .eq("id", tripId)
    .maybeSingle();
  return data;
}

async function memberIds(supabase: SupabaseClient, tripId: string, role?: string): Promise<string[]> {
  let query = supabase.from("trip_access").select("user_id, role").eq("trip_id", tripId);
  if (role) query = query.eq("role", role);
  const { data } = await query;
  return (data ?? []).map((row) => text(row.user_id)).filter(Boolean);
}

function owedByMember(members: Row[], contributions: Row[]): Map<string, { name: string; owed: number }> {
  const paid = new Map<string, number>();
  for (const row of contributions) {
    const id = text(row.member_id);
    paid.set(id, (paid.get(id) ?? 0) + number(row.amount_paise));
  }
  const result = new Map<string, { name: string; owed: number }>();
  for (const member of members) {
    const id = text(member.id);
    if (!id) continue;
    result.set(id, {
      name: text(member.name) || "Someone",
      owed: number(member.expected_contribution_paise) - (paid.get(id) ?? 0),
    });
  }
  return result;
}

function removeId(rows: Row[], id: string) {
  const index = rows.findIndex((row) => text(row.id) === id);
  if (index >= 0) rows.splice(index, 1);
}

function replaceId(rows: Row[], id: string, next: Row) {
  const index = rows.findIndex((row) => text(row.id) === id);
  if (index >= 0) rows[index] = next;
}

function sum(rows: Row[], field: string): number {
  return rows.reduce((total, row) => total + number(row[field]), 0);
}

function text(value: unknown): string {
  return typeof value === "string" ? value : value == null ? "" : String(value);
}

function number(value: unknown): number {
  const parsed = typeof value === "number" ? value : Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatInr(paise: number): string {
  const rupees = Math.round(paise / 100);
  return `₹${rupees.toLocaleString("en-IN")}`;
}
