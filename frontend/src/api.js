export async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (response.ok) return response.status === 204 ? null : response.json();

  let message = `${url} returned ${response.status}`;
  try {
    const body = await response.json();
    if (body.message) message = body.message;
  } catch {
    // The status-based message remains useful when an endpoint returns no JSON body.
  }
  throw new Error(message);
}
