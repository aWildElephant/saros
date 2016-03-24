/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *  (c) NFQ (www.nfq.com) - 2014
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.util.ThrowableComputable;
import de.fu_berlin.inf.dpp.filesystem.IFile;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * IDEA implementation of the IFile interface.
 */
public class IntelliJFileImpl extends IntelliJResourceImpl implements IFile {

    private static Logger LOG = Logger.getLogger(IntelliJFileImpl.class);

    public IntelliJFileImpl(IntelliJProjectImpl project, File file) {
        super(project, file);
    }

    @Override
    public String getCharset() throws IOException {
        return getDefaultCharset();
    }

    @Override
    public InputStream getContents() throws IOException {
        return getVirtualFile().getInputStream();
    }

    @Override
    public void setContents(final InputStream input, boolean force,
        boolean keepHistory) throws IOException {
        final OutputStream fos = getVirtualFile().getOutputStream(this);

        writeInUIThread(new ThrowableComputable<Void, IOException>() {
            @Override
            public Void compute() throws IOException {
                try {
                    int read;
                    byte[] buffer = new byte[1024];
                    while ((read = input.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                } finally {
                    fos.flush();
                    fos.close();
                }

                return null;
            }
        });
    }

    @Override
    public void create(InputStream input, boolean force) throws IOException {
        writeInUIThread(new ThrowableComputable<Void, IOException>() {

            @Override
            public Void compute() throws IOException {
                getParent().getVirtualFile().createChildData(this, getName());
                return null;
            }
        });

        setContents(input, true, true);
        LOG.trace("Created file " + this);
    }

    @Override
    public long getSize() throws IOException {
        return getVirtualFile().getLength();
    }

    @Override
    public void refreshLocal() throws IOException {
        getVirtualFile().refresh(false, false);
    }

    @Override
    public int getType() {
        return FILE;
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }

    @Override
    public String toString() {
        return projectRelativePath.toPortableString();
    }
}
