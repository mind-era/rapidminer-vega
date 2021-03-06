/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.local;

import java.io.File;
import java.io.IOException;

import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;
/**
 * @author Simon Fischer
 */
public class SimpleProcessEntry extends SimpleDataEntry implements ProcessEntry {


	SimpleProcessEntry(String name, SimpleFolder containingFolder, LocalRepository repository) {
		super(name, containingFolder, repository);
	}

	@Override
	public String retrieveXML() throws RepositoryException {
		try {
			return Tools.readTextFile(getFile());
		} catch (IOException e) {
			throw new RepositoryException("Cannot read "+getFile() + ": "+e, e);
		}
	}

	@Override
	public void storeXML(String xml) throws RepositoryException {
		try {
			Tools.writeTextFile(getFile(), xml);
		} catch (IOException e) {
			throw new RepositoryException("Cannot write "+getFile() + ": "+e, e);
		}
	}

	private File getFile() {
		return new File(((SimpleFolder)getContainingFolder()).getFile(), getName()+".rmp");
	}

	@Override
	public int getRevision() {
		return 1;
	}

	@Override
	public long getSize() {	
		return getFile().length();
	}

	@Override
	public void delete() throws RepositoryException {
		getFile().delete();
		super.delete();
	}

	@Override
	public String getDescription() {
		return "Local process";
	}

	@Override
	public String getType() {		
		return ProcessEntry.TYPE_NAME;
	}
	
	@Override
	public boolean rename(String newName) {
		renameFile(getFile(), newName);
		return super.rename(newName);
	}
	
	@Override
	public boolean move(Folder newParent) {		
		moveFile(getFile(), ((SimpleFolder)newParent).getFile());
		return super.move(newParent);
	}

	@Override
	public long getDate() {
		return getFile().lastModified();
	}

}
