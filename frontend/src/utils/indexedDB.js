const DB_NAME = 'ai_drawing_local'
const DB_VERSION = 1
const STORE_NAME = 'drawings'
const MAX_RECORDS = 50

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, {
          keyPath: 'id',
          autoIncrement: true,
        })
        store.createIndex('createdAt', 'createdAt', { unique: false })
      }
    }
    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => reject(event.target.error)
  })
}

export async function addDrawing(drawingData) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)

    const record = {
      ...drawingData,
      createdAt: drawingData.createdAt || new Date().toISOString(),
    }

    const addRequest = store.add(record)
    addRequest.onsuccess = async () => {
      const count = await countDrawings()
      if (count > MAX_RECORDS) {
        await trimOldest(count - MAX_RECORDS)
      }
      resolve(addRequest.result)
    }
    addRequest.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

export async function getAllDrawings() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const index = store.index('createdAt')
    const request = index.getAll()
    request.onsuccess = (event) => {
      const results = event.target.result
      results.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      resolve(results)
    }
    request.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

export async function getDrawing(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const request = store.get(id)
    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

export async function deleteDrawing(id) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.delete(id)
    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

export async function clearAllDrawings() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.clear()
    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

async function countDrawings() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const request = store.count()
    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}

async function trimOldest(count) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const index = store.index('createdAt')
    const cursorRequest = index.openCursor(null, 'next')

    let deleted = 0
    cursorRequest.onsuccess = (event) => {
      const cursor = event.target.result
      if (cursor && deleted < count) {
        cursor.delete()
        deleted++
        cursor.continue()
      } else {
        resolve()
      }
    }
    cursorRequest.onerror = (event) => reject(event.target.error)
    tx.oncomplete = () => db.close()
  })
}
