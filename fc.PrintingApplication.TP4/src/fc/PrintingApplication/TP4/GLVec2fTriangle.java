package fc.PrintingApplication.TP4;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import fc.Math.Vec2f;

public class GLVec2fTriangle
{
	public int m_VBO;
	public int m_IBO;
	private FloatBuffer m_AttributesBuffer;
	private IntBuffer m_IndicesBuffer;
	
	public GLVec2fTriangle(Vec2f[] v) // must be 3 vertices
	{
		m_VBO = glGenBuffers();
		m_IBO = glGenBuffers();

		m_AttributesBuffer = BufferUtils.createFloatBuffer(9);
		m_AttributesBuffer.put(v[0].x); m_AttributesBuffer.put(v[0].y); m_AttributesBuffer.put(0.0f);
		m_AttributesBuffer.put(v[1].x); m_AttributesBuffer.put(v[1].y); m_AttributesBuffer.put(0.0f);
		m_AttributesBuffer.put(v[2].x); m_AttributesBuffer.put(v[2].y); m_AttributesBuffer.put(0.0f);

		m_IndicesBuffer = BufferUtils.createIntBuffer(3);
		m_IndicesBuffer.put(0); m_IndicesBuffer.put(1); m_IndicesBuffer.put(2);
				
		glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)m_AttributesBuffer.flip(), GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_IBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer)m_IndicesBuffer.flip(), GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
	}
	
	public GLVec2fTriangle(List<Vec2f> poly) {
		m_VBO = glGenBuffers();
		m_IBO = glGenBuffers();
		
		m_AttributesBuffer = BufferUtils.createFloatBuffer(poly.size() * 3);
		for(int i = 0; i < poly.size(); ++i) {
			m_AttributesBuffer.put(poly.get(i).x);
			m_AttributesBuffer.put(poly.get(i).y);
			m_AttributesBuffer.put(0.0f);
		}
		
		// nb triangle fan = nb vertices in polygone - 2
		int capacity = poly.size() - 2;
		
		
		m_IndicesBuffer = BufferUtils.createIntBuffer(capacity * 3);
		int si = 0;

			int size = poly.size();
			int origin = 0;
			for(int tri = origin + 1; tri < size -1; ++tri) {
				++si; 
				m_IndicesBuffer.put(origin);
				m_IndicesBuffer.put(tri);
				m_IndicesBuffer.put(tri+1);
			}
		
		glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)m_AttributesBuffer.flip(), GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_IBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer)m_IndicesBuffer.flip(), GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
	}
	
	public GLVec2fTriangle(Slice slice) {
		m_VBO = glGenBuffers();
		m_IBO = glGenBuffers();
		
		m_AttributesBuffer = BufferUtils.createFloatBuffer(slice.edges.size() * 3);
		for(int i = 0; i < slice.edges.size(); ++i) {
			m_AttributesBuffer.put(slice.edges.get(i).x);
			m_AttributesBuffer.put(slice.edges.get(i).y);
			m_AttributesBuffer.put(0.0f);
		}
		
		int capacity = 0;
		// nb triangle fan = nb vertices in polygone - 2
		int ofst =0;
		for(int nbVert : slice.islands) {
			capacity += Math.max(0, (nbVert - ofst) -2);
			ofst = nbVert;
		}
		
		m_IndicesBuffer = BufferUtils.createIntBuffer(capacity * 3);
		int si = 0;
		for(int i = 0; i < slice.islands.size() -1; ++i) {
			int size = slice.islands.get(i+1);
			int origin = slice.islands.get(i);
			for(int tri = origin + 1; tri < size -1; ++tri) {
				++si; 
				m_IndicesBuffer.put(origin);
				m_IndicesBuffer.put(tri);
				m_IndicesBuffer.put(tri+1);
			}
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)m_AttributesBuffer.flip(), GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_IBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer)m_IndicesBuffer.flip(), GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
	}
	
	public void render()
	{
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);	
		
		glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_IBO);

		glDrawElements(GL11.GL_TRIANGLES, m_IndicesBuffer.capacity(), GL_UNSIGNED_INT, 0);

		
		glDisableVertexAttribArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

	//	GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
	//	GL11.glStencilMask(0xFF);

	}
	
	public void dispose()
	{
		GL15.glDeleteBuffers(m_IBO);
		GL15.glDeleteBuffers(m_VBO);
	}
}
