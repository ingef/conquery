/*

This has been taken from
https://github.com/zeit/async-sema/blob/master/src/index.ts

Originally, this library was only made for node.
To make it work in the browser, 'events' has been installed.

*/

class EventEmitter {
  callbacks: Record<string, ((arg?: string) => void)[]>;
  constructor() {
    this.callbacks = {};
  }

  on(event: string, cb: (arg?: string) => void) {
    if (!this.callbacks[event]) this.callbacks[event] = [];
    this.callbacks[event].push(cb);
  }

  emit(event: string, data?: string) {
    const cbs = this.callbacks[event];
    if (cbs) {
      cbs.forEach((cb) => cb(data));
    }
  }
}

function arrayMove(
  src: unknown[],
  srcIndex: number,
  dst: unknown[],
  dstIndex: number,
  len: number,
) {
  for (let j = 0; j < len; ++j) {
    dst[j + dstIndex] = src[j + srcIndex];
    src[j + srcIndex] = void 0;
  }
}

function pow2AtLeast(n: number) {
  n = n >>> 0;
  n = n - 1;
  n = n | (n >> 1);
  n = n | (n >> 2);
  n = n | (n >> 4);
  n = n | (n >> 8);
  n = n | (n >> 16);
  return n + 1;
}

function getCapacity(capacity: number) {
  return pow2AtLeast(Math.min(Math.max(16, capacity), 1073741824));
}

// Deque is based on https://github.com/petkaantonov/deque/blob/master/js/deque.js
// Released under the MIT License: https://github.com/petkaantonov/deque/blob/6ef4b6400ad3ba82853fdcc6531a38eb4f78c18c/LICENSE
class Deque {
  _capacity: number;
  _length: number;
  _front: number;
  arr: Array<unknown>;

  constructor(capacity: number) {
    this._capacity = getCapacity(capacity);
    this._length = 0;
    this._front = 0;
    this.arr = [];
  }

  push(item: unknown): number {
    const length = this._length;

    this.checkCapacity(length + 1);
    const i = (this._front + length) & (this._capacity - 1);
    this.arr[i] = item;
    this._length = length + 1;

    return length + 1;
  }

  pop() {
    const length = this._length;
    if (length === 0) {
      return void 0;
    }
    const i = (this._front + length - 1) & (this._capacity - 1);
    const ret = this.arr[i];
    this.arr[i] = void 0;
    this._length = length - 1;

    return ret;
  }

  shift() {
    const length = this._length;
    if (length === 0) {
      return void 0;
    }
    const front = this._front;
    const ret = this.arr[front];
    this.arr[front] = void 0;
    this._front = (front + 1) & (this._capacity - 1);
    this._length = length - 1;

    return ret;
  }

  get length(): number {
    return this._length;
  }

  checkCapacity(size: number) {
    if (this._capacity < size) {
      this.resizeTo(getCapacity(this._capacity * 1.5 + 16));
    }
  }

  resizeTo(capacity: number) {
    const oldCapacity = this._capacity;
    this._capacity = capacity;
    const front = this._front;
    const length = this._length;
    if (front + length > oldCapacity) {
      const moveItemsCount = (front + length) & (oldCapacity - 1);
      arrayMove(this.arr, 0, this.arr, oldCapacity, moveItemsCount);
    }
  }
}

class ReleaseEmitter extends EventEmitter {}

function isFn(x: unknown) {
  return typeof x === "function";
}

function defaultInit() {
  return "1";
}

export class Sema {
  nrTokens: number;
  free: Deque;
  waiting: Deque;
  releaseEmitter: EventEmitter;
  noTokens: boolean;
  pauseFn?: () => void;
  resumeFn?: () => void;
  paused: boolean;

  constructor(
    nr: number,
    {
      initFn = defaultInit,
      pauseFn,
      resumeFn,
      capacity = 10,
    }: {
      initFn?: () => unknown;
      pauseFn?: () => void;
      resumeFn?: () => void;
      capacity?: number;
    } = {},
  ) {
    if (isFn(pauseFn) !== isFn(resumeFn)) {
      throw new Error("pauseFn and resumeFn must be both set for pausing");
    }

    this.nrTokens = nr;
    this.free = new Deque(nr);
    this.waiting = new Deque(capacity);
    this.releaseEmitter = new ReleaseEmitter();
    this.noTokens = initFn === defaultInit;
    this.pauseFn = pauseFn;
    this.resumeFn = resumeFn;
    this.paused = false;

    this.releaseEmitter.on("release", (token?: string) => {
      const p = this.waiting.shift();
      if (p) {
        // @ts-ignore // TODO: either type or fully ignore this file
        p.resolve(token);
      } else {
        if (this.resumeFn && this.paused) {
          this.paused = false;
          this.resumeFn();
        }

        this.free.push(token);
      }
    });

    for (let i = 0; i < nr; i++) {
      this.free.push(initFn());
    }
  }

  async acquire(): Promise<unknown> {
    const token = this.free.pop();

    if (token !== void 0) {
      return token;
    }

    return new Promise((resolve, reject) => {
      if (this.pauseFn && !this.paused) {
        this.paused = true;
        this.pauseFn();
      }

      this.waiting.push({ resolve, reject });
    });
  }

  release(token?: string): void {
    this.releaseEmitter.emit("release", this.noTokens ? "1" : token);
  }

  drain(): Promise<unknown[]> {
    const a = new Array(this.nrTokens);
    for (let i = 0; i < this.nrTokens; i++) {
      a[i] = this.acquire();
    }
    return Promise.all(a);
  }

  nrWaiting(): number {
    return this.waiting.length;
  }
}
