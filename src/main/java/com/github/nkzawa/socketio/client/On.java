package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;

public class On extends Emitter {

    private On() {}

    public static Handle on(final Emitter obj, final String ev, final Listener fn) {
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
