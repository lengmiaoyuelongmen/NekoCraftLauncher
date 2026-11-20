package com.example.demo;

import com.example.demo.core.server.UpnpPortMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UPnP 里最容易出错的两段纯逻辑：SSDP 响应解析 与 SOAP 信封拼装。 */
class UpnpPortMapperTest {

    @Test
    void locationIsReadFromSsdpHeadersCaseInsensitively() {
        String response = "HTTP/1.1 200 OK\r\n"
                + "CACHE-CONTROL: max-age=120\r\n"
                + "LOCATION: http://192.168.1.1:5000/rootDesc.xml\r\n"
                + "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n\r\n";
        assertEquals("http://192.168.1.1:5000/rootDesc.xml", UpnpPortMapper.parseLocation(response));
    }

    @Test
    void locationParsingToleratesLowercaseAndMissingHeader() {
        assertEquals("http://10.0.0.1/desc.xml",
                UpnpPortMapper.parseLocation("location:   http://10.0.0.1/desc.xml  \r\n"));
        assertNull(UpnpPortMapper.parseLocation("HTTP/1.1 200 OK\r\nST: something\r\n"));
        assertNull(UpnpPortMapper.parseLocation(null));
    }

    @Test
    void soapEnvelopeWrapsActionAndArguments() {
        String xml = UpnpPortMapper.soapEnvelope("urn:schemas-upnp-org:service:WANIPConnection:1",
                "AddPortMapping", "<NewExternalPort>25565</NewExternalPort>");
        assertTrue(xml.contains("s:Envelope"));
        assertTrue(xml.contains("<u:AddPortMapping xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">"));
        assertTrue(xml.contains("<NewExternalPort>25565</NewExternalPort>"));
        assertTrue(xml.contains("</u:AddPortMapping></s:Body></s:Envelope>"));
    }

    @Test
    void natDetectionComparesPublicAndLocalAddresses() {
        assertTrue(UpnpPortMapper.behindNat("125.113.214.74", "192.168.1.6"));
        assertTrue(!UpnpPortMapper.behindNat("192.168.1.6", "192.168.1.6"));
        assertTrue(!UpnpPortMapper.behindNat("", "192.168.1.6"), "拿不到公网 IP 时不下结论");
    }
}
