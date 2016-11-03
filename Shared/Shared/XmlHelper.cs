using System.IO;
using System.Text;
using System.Xml.Linq;

namespace Shared
{
    public class XmlHelper
    {
        public XDocument ByteArrayToXml(byte[] stream)
        {
            if (stream == null || stream.Length < 4)
            {
                return null;
            }            
            var responseText = Encoding.UTF8.GetString(stream, 4, stream.Length - 4);            
            var responseXml = XDocument.Load(responseText);
            return responseXml;
        }

    }
}
