// ========================================================================
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.i2p.orchid.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.i2p.I2PAppContext;
import net.i2p.data.ByteArray;
import net.i2p.data.DataHelper;
import net.i2p.util.ByteCache;
import net.i2p.util.Log;
import net.i2p.util.SystemVersion;


/* ------------------------------------------------------------ */
/**
 * Based on DefaultServlet from Jetty 6.1.26, heavily simplified
 * and modified to remove all dependencies on Jetty libs.
 *
 * Supports HEAD and GET only, for resources from the .war and local files.
 * Supports files and resource only.
 * Supports MIME types with local overrides and additions.
 * Supports Last-Modified.
 * Supports single request ranges.
 *
 * Does not support directories or "welcome files".
 * Does not support gzip.
 * Does not support multiple request ranges.
 * Does not cache.
 *
 * POST returns 405.
 * Directories return 403.
 * Jar resources are sent with a long cache directive.
 *
 * ------------------------------------------------------------ 
 *
 * The default servlet.                                                 
 * This servlet, normally mapped to /, provides the handling for static 
 * content, OPTION and TRACE methods for the context.                   
 * The following initParameters are supported, these can be set
 * on the servlet itself:
 * <PRE>                                                                      
 *
 *  resourceBase      Set to replace the context resource base

 *  warBase      Path allowed for resource in war
 * 
 * </PRE>
 *                                                                    
 *
 * @author Greg Wilkins (gregw)
 * @author Nigel Canonizado
 *                                                                    
 * @since Jetty 7
 */
class BasicServlet extends HttpServlet
{   
    protected final I2PAppContext _context;
    protected final Log _log;
    protected File _resourceBase;
    private String _warBase;
    
    /** same as PeerState.PARTSIZE */
    private static final int BUFSIZE = 16*1024;
    private ByteCache _cache = ByteCache.getInstance(16, BUFSIZE);

    private static final int WAR_CACHE_CONTROL_SECS = 24*60*60;
    private static final int FILE_CACHE_CONTROL_SECS = 24*60*60;

    public BasicServlet() {
        super();
        _context = I2PAppContext.getGlobalContext();
        _log = _context.logManager().getLog(getClass());
    }
    
    /* ------------------------------------------------------------ */
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
    }

    /* ------------------------------------------------------------ */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.sendError(405);
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doTrace(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.sendError(405);
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.sendError(405);
    }
    
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.sendError(405);
    }

    /**
     *  Simple version of URIUtil.addPaths()
     *  @param path may be null
     */
    protected static String addPaths(String base, String path) {
        if (path == null)
            return base;
        String rv = (new File(base, path)).toString();
        if (SystemVersion.isWindows())
            rv = rv.replace("\\", "/");
        return rv;
    }
}
