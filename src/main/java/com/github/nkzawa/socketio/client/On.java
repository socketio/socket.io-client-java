package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;

public class On {

    private On() {}

    public static Handle on(final Emitter obj, final String ev, final Emitter.Listener fn) {
        obj.on(ev, fn);
        return new Handle() {
            @Override
            public void destroy() {
                obj.off(ev, fn);
            }
        };
    }

    public static interface Handle {

        public void destroy();
    }
}
