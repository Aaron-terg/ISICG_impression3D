package fc.PrintingApplication.TP4;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import fc.GLObjects.GLError;
import fc.GLObjects.GLRenderTarget;

public class GLRenderTargetStencil extends GLRenderTarget{
	
	protected int m_IdDepthView;
	protected int m_IdDepthStencil;
	public GLRenderTargetStencil(int width, int height, int internalFormat, int format, int type) {
		super(width, height, internalFormat, format, type);
		// TODO Auto-generated constructor stub
		
		
		m_IdDepthStencil = GL11.glGenTextures();
	  	GL11.glBindTexture(GL11.GL_TEXTURE_2D, m_IdDepthStencil);
		GLError.check("Could not bind texture ID");
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GLError.check("Call to getTexParameteri GL_TEXTURE_MIN_FILTER failed");
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GLError.check("Call to getTexParameteri GL_TEXTURE_MAG_FILTER failed");
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GLError.check("Call to getTexParameteri GL_TEXTURE_WRAP_S failed");
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GLError.check("Call to getTexParameteri GL_TEXTURE_WRAP_T failed");
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D,
				0, // level,
				GL30.GL_DEPTH24_STENCIL8,
				width, height,
				0, // border, must always be 0
				GL30.GL_DEPTH_STENCIL,
				GL30.GL_UNSIGNED_INT_24_8,
				0L);
		GLError.check("Could not create texture used as depth buffer back-end for framebuffer");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GLError.check("Could not bind texture ID");
		
		
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, m_FboId);
		GLError.check("Could not bind framebuffer");
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, m_IdDepthStencil, 0);
		GLError.check("Could not bind stencil attachment to framebuffer");
		
	/*	int DepthStencilRenderBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, m_IdDepthStencil);
		GLError.check("Could not bind renderbuffer");
		
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height);
		GLError.check("Could not store renderbuffer");
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
		GLError.check("Could not unbind renderbuffer");
		*/
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
			throw new IllegalStateException("glCheckFramebufferStatus returned " + status);
		GLError.check("glCheckFramebufferStatus returned GL_FRAMEBUFFER_COMPLETE but OpenGL is now in error state");

		// https://stackoverflow.com/questions/27535727/opengl-create-a-depth-stencil-texture-for-reading
	/*	m_IdDepthView = GL11.glGenTextures();
		GL44.glTextureView(m_IdDepthView, GL11.GL_TEXTURE_2D, m_IdDepthStencil, GL30.GL_DEPTH24_STENCIL8, 0, 1, 0, 1);
		GLError.check("Could not create texture view");
		*/
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GLError.check("Could not unbind framebuffer");
	}
	
	
	private int getSizeOfOneComponentsInBytes()
	{
		switch (m_InternalFormat)
		{
		case GL30.GL_R32F:
		case GL30.GL_R32I:
		case GL30.GL_R32UI:
		case GL30.GL_RGBA32I:
		case GL30.GL_RGBA32UI:
		case GL30.GL_RGBA32F:
		case GL30.GL_DEPTH24_STENCIL8:
			return 4;
		default:
			throw new IllegalStateException("Don't know how to calculate the size of internal OpenGL pixel format " + m_InternalFormat);
		}
	}
	
	private int getNumPixelFormatComponents()
	{
		switch (m_InternalFormat)
		{
		case GL30.GL_R32F:
		case GL30.GL_R32I:
		case GL30.GL_R32UI:
			return 1;
		case GL30.GL_RGBA32I:
		case GL30.GL_RGBA32UI:
		case GL30.GL_RGBA32F:
		case GL30.GL_DEPTH24_STENCIL8:
			return 4;
		default:
			throw new IllegalStateException("Don't know how to calculate the number of pixel format components for OpenGL pixel format " + m_InternalFormat);
		}
	}
	
	
	public void dispose()
	{
		GL11.glDeleteTextures(m_IdDepth);
		GLError.check("Could not delete texture " + m_IdDepth);
		GL11.glDeleteTextures(m_IdTex);
		GLError.check("Could not delete texture " + m_IdTex);
	
		/*GL11.glDeleteTextures(m_IdDepthView);
		GLError.check("Could not delete texture " + m_IdDepthView);
		*/
		GL11.glDeleteTextures(m_IdDepthStencil);
		GLError.check("Could not delete texture " + m_IdDepthStencil);
		GL30.glDeleteFramebuffers(m_FboId);
		GLError.check("Could not delete framebuffer " + m_FboId);
	}
	
	public int getDepthStencilTexId()
	{
		return m_IdDepthStencil;
	}
	
	public int getDepthStencilViewTexId()
	{
		return m_IdDepthView;
	}
	

}
