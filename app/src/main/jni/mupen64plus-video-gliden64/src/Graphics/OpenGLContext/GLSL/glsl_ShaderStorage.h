#pragma once
#include <Graphics/OpenGLContext/opengl_GLInfo.h>

namespace opengl {
	class CachedUseProgram;
}

namespace glsl {

	class ShaderStorage
	{
	public:
		ShaderStorage(const opengl::GLInfo & _glinfo, opengl::CachedUseProgram * _useProgram);

		bool saveShadersStorage(const graphics::Combiners & _combiners) const;

		bool loadShadersStorage(graphics::Combiners & _combiners);

	private:
		bool _saveCombinerKeys(const graphics::Combiners & _combiners) const;
		bool _loadFromCombinerKeys(graphics::Combiners & _combiners);

		const u32 m_formatVersion = 0x15U;
		const u32 m_keysFormatVersion = 0x02;
		const opengl::GLInfo & m_glinfo;
		opengl::CachedUseProgram * m_useProgram;
	};

}
