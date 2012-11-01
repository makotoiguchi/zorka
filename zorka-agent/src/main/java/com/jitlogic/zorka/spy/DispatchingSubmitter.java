/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.spy.collectors.SpyCollector;
import com.jitlogic.zorka.vmsci.SpySubmitter;

import java.util.Stack;

import static com.jitlogic.zorka.spy.SpyConst.*;

import static com.jitlogic.zorka.spy.SpyConst.ON_SUBMIT;

/**
 *
 */
public class DispatchingSubmitter implements SpySubmitter {

    private InstrumentationEngine engine;
    private SpyCollector collector;

    private ThreadLocal<Stack<SpyRecord>> submissionStack =
        new ThreadLocal<Stack<SpyRecord>>() {
            @Override
            public Stack<SpyRecord> initialValue() {
                return new Stack<SpyRecord>();
            }
        };


    public DispatchingSubmitter(InstrumentationEngine engine, SpyCollector collector) {
        this.engine = engine;
        this.collector = collector;
    }


    public void submit(int stage, int id, int submitFlags, Object[] vals) {
        InstrumentationContext ctx = engine.getContext(id);

        if (ctx == null) {
            return;
        }

        SpyRecord record = getRecord(stage, ctx, submitFlags, vals);

        SpyDefinition sdef = ctx.getSpyDefinition();

        if (null == (record = SpyUtil.transform(stage, sdef, record))) {
            return;
        }

        if (submitFlags == SF_NONE) {
            submissionStack.get().push(record);
            return;
        }

        record.beforeSubmit();

        if (null == (record = SpyUtil.transform(ON_SUBMIT, sdef, record))) {
            return;
        }

        record.beforeCollect();

        collector.collect(record);
    }


    private SpyRecord getRecord(int stage, InstrumentationContext ctx, int submitFlags, Object[] vals) {

        SpyRecord record = null;

        switch (submitFlags) {
            case SF_IMMEDIATE:
            case SF_NONE:
                record = new SpyRecord(ctx);
                break;
            case SF_FLUSH:
                Stack<SpyRecord> stack = submissionStack.get();
                if (stack.size() > 0) {
                    record = stack.pop();
                    // TODO check if record belongs to proper frame, warn if not
                } // TODO warn if there was no record on stack
        }

        record.feed(stage, vals);

        return record;
    }
}