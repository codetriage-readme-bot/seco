package seco.notebook.javascript;

import java.util.LinkedList;
import java.util.List;

import seco.notebook.ruby.RubyParser;
import seco.notebook.syntax.Mode;
import seco.notebook.syntax.ScriptSupport;
import seco.notebook.syntax.completion.CompletionProvider;
import seco.notebook.syntax.completion.NBParser;

public class JSScriptSupport extends ScriptSupport
{
    private static List<Mode> modes = new LinkedList<Mode>();
    static
    {
        modes.add(new Mode("javascript", "/modes/javascript.xml"));
    }

    @Override
    public String getModeName()
    {
        return "javascript";
    }

    @Override
    public List<Mode> getModes()
    {
        return modes;
    }

    @Override
    public String getScriptEngineName()
    {
        return "javascript";
    }

    @Override
    public CompletionProvider[] getCompletionProviders()
    {
        return null;
    }

    private JSParser parser = null;

    @Override
    public NBParser getParser()
    {
        if (parser == null)
        {
            parser = new JSParser(this);
        }
        return parser;
    }

}