/**
 * Copyright (c) 2013, impossibl.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of impossibl.com nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.impossibl.postgres.jdbc;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;



/*
 * Test that enhanced error reports return the correct origin
 * for constraint violation errors.
 */
public class ServerErrorTest extends TestCase {

	private Connection con;

	public ServerErrorTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {

		con = TestUtil.openDB();
		
		Statement stmt = con.createStatement();
		stmt.execute("CREATE DOMAIN testdom AS int4 CHECK (value < 10)");

		TestUtil.createTable(con, "testerr", "id int not null, val testdom not null");
		
		stmt.execute("ALTER TABLE testerr ADD CONSTRAINT testerr_pk PRIMARY KEY (id)");
		stmt.close();
	}

	protected void tearDown() throws Exception {
		
		TestUtil.dropTable(con, "testerr");
		
		Statement stmt = con.createStatement();
		stmt.execute("DROP DOMAIN testdom");
		stmt.close();
		
		TestUtil.closeDB(con);
	}

	public void testPrimaryKey() throws Exception {
		
		Statement stmt = con.createStatement();
		stmt.executeUpdate("INSERT INTO testerr (id, val) VALUES (1, 1)");
		
		try {
			stmt.executeUpdate("INSERT INTO testerr (id, val) VALUES (1, 1)");
			fail("Should have thrown a duplicate key exception.");
		}
		catch (PGSQLException sqle) {
			assertEquals("public", sqle.getSchema());
			assertEquals("testerr", sqle.getTable());
			assertEquals("testerr_pk", sqle.getConstraint());
			assertNull(sqle.getDatatype());
			assertNull(sqle.getColumn());
		}
		
		stmt.close();
	}

	public void testColumn() throws Exception {
		
		Statement stmt = con.createStatement();
		
		try {
			stmt.executeUpdate("INSERT INTO testerr (id, val) VALUES (1, NULL)");
			fail("Should have thrown a not null constraint violation.");
		}
		catch (PGSQLException sqle) {
			assertEquals("public", sqle.getSchema());
			assertEquals("testerr", sqle.getTable());
			assertEquals("val", sqle.getColumn());
			assertNull(sqle.getDatatype());
			assertNull(sqle.getConstraint());
		}
		
		stmt.close();
	}

	public void testDatatype() throws Exception {
		
		Statement stmt = con.createStatement();
		
		try {
			stmt.executeUpdate("INSERT INTO testerr (id, val) VALUES (1, 20)");
			fail("Should have thrown a constraint violation.");
		}
		catch (PGSQLException sqle) {
			assertEquals("public", sqle.getSchema());
			assertEquals("testdom", sqle.getDatatype());
			assertEquals("testdom_check", sqle.getConstraint());
		}
		
		stmt.close();
	}

}
